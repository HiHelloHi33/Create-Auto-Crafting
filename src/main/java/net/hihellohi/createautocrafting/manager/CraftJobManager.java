package net.hihellohi.createautocrafting.manager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.network.CraftDoneToastPacket;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs multi-step craft jobs. A job is expanded into steps bottom-up (a step per craftable item that
 * must be produced, children before parents). Each tick, a step ships its ingredients to its Crafting
 * Distributor as soon as they are all present on the network; once that step's result returns to the
 * network (the player crafts and stores it), parent steps unlock. The final (root) step completing
 * fires the "craft finished" toast. Ingredients are requested at the network level, so the stock
 * ticker's address field is never touched.
 */
public final class CraftJobManager {
    private static final int MAX_STEPS = 64;
    private static final int MAX_DEPTH = 24;
    private static final long TIMEOUT_TICKS = 20L * 60 * 30; // 30 minutes

    private static final class Step {
        final ItemStack result;
        final int needed;                 // result items this step must produce
        final BlockPos provider;
        final String address;
        final List<BigItemStack> ingredients;
        boolean shipped;
        boolean done;
        int baseline;

        Step(ItemStack result, int needed, BlockPos provider, String address, List<BigItemStack> ingredients) {
            this.result = result;
            this.needed = needed;
            this.provider = provider;
            this.address = address;
            this.ingredients = ingredients;
        }
    }

    private static final class Job {
        final UUID player;
        final ResourceKey<Level> dimension;
        final UUID networkId;
        final ItemStack rootResult;
        final int rootAmount;
        final List<Step> steps; // children first, root last
        final long createdTick;

        Job(UUID player, ResourceKey<Level> dimension, UUID networkId, ItemStack rootResult,
            int rootAmount, List<Step> steps, long createdTick) {
            this.player = player;
            this.dimension = dimension;
            this.networkId = networkId;
            this.rootResult = rootResult;
            this.rootAmount = rootAmount;
            this.steps = steps;
            this.createdTick = createdTick;
        }
    }

    private static final ConcurrentLinkedQueue<Job> JOBS = new ConcurrentLinkedQueue<>();

    private CraftJobManager() {}

    /** Build and queue a job. Returns false if the tree can't be planned (missing raw materials). */
    public static boolean start(ServerLevel level, UUID networkId, UUID player, ItemStack result, int amount) {
        List<PackagerBlockEntity> packagers = NetworkInventories.packagers(level, networkId);
        List<Step> steps = new ArrayList<>();
        Map<String, Integer> reserved = new HashMap<>();
        boolean ok = expand(level, networkId, packagers, reserved, result, Math.max(1, amount),
                new HashSet<>(), 0, steps);
        if (!ok || steps.isEmpty()) {
            return false;
        }
        JOBS.add(new Job(player, level.dimension(), networkId, result.copy(), amount, steps, level.getGameTime()));
        return true;
    }

    private static boolean expand(ServerLevel level, UUID networkId, List<PackagerBlockEntity> packagers,
                                  Map<String, Integer> reserved, ItemStack result, int amount,
                                  Set<String> inProgress, int depth, List<Step> steps) {
        if (depth > MAX_DEPTH || steps.size() > MAX_STEPS) {
            return false;
        }
        String key = key(result);
        int total = NetworkInventories.count(packagers, result);
        int avail = reserved.computeIfAbsent(key, k -> total);
        int toCraft;
        if (depth == 0) {
            // Top-level request (the end result): always craft the full requested amount, even when the
            // network already stocks that many — "request 16" means make 16 more, not "top up to 16".
            // Existing stock stays reserved so ingredient branches can still draw from it.
            toCraft = amount;
        } else {
            int use = Math.min(avail, amount);
            reserved.put(key, avail - use);
            toCraft = amount - use;
        }
        if (toCraft <= 0) {
            return true; // enough already in stock; no step needed
        }
        CraftingPattern pattern = PatternRegistry.getPatternForResult(level, networkId, result);
        if (pattern == null) {
            return false; // raw material the network is short on
        }
        if (!inProgress.add(key)) {
            return false; // cycle
        }
        try {
            int per = Math.max(1, pattern.outputCount());
            int crafts = (toCraft + per - 1) / per;
            List<ItemStack> merged = pattern.mergedIngredients(); // combine duplicate slots
            for (ItemStack ingredient : merged) {
                if (!expand(level, networkId, packagers, reserved, ingredient,
                        Math.max(1, ingredient.getCount()) * crafts, inProgress, depth + 1, steps)) {
                    return false;
                }
            }
            BlockPos provider = PatternRegistry.getProviderForResult(level, networkId, result);
            if (provider == null) {
                return false;
            }
            String address = PatternProviderHelper.resolveAddress(level, provider);
            List<BigItemStack> ingredients = new ArrayList<>();
            for (ItemStack ingredient : merged) {
                ingredients.add(new BigItemStack(ingredient.copyWithCount(1),
                        Math.max(1, ingredient.getCount()) * crafts));
            }
            steps.add(new Step(result.copyWithCount(1), toCraft, provider, address, ingredients));
            return true;
        } finally {
            inProgress.remove(key);
        }
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                continue;
            }
            if (level.getGameTime() - job.createdTick > TIMEOUT_TICKS) {
                it.remove();
                continue;
            }
            List<PackagerBlockEntity> packagers = NetworkInventories.packagers(level, job.networkId);

            // Mark shipped steps whose result has returned to the network.
            for (Step step : job.steps) {
                if (step.shipped && !step.done
                        && NetworkInventories.count(packagers, step.result) - step.baseline >= step.needed) {
                    step.done = true;
                }
            }
            // Ship steps whose ingredients are all available (children produce inputs for parents).
            for (Step step : job.steps) {
                if (step.shipped || step.done) {
                    continue;
                }
                if (ingredientsAvailable(step, packagers)) {
                    step.baseline = NetworkInventories.count(packagers, step.result);
                    ship(job.networkId, step);
                    step.shipped = true;
                }
            }

            Step root = job.steps.get(job.steps.size() - 1);
            if (root.done) {
                ServerPlayer player = server.getPlayerList().getPlayer(job.player);
                if (player != null) {
                    ModPackets.sendToPlayer(new CraftDoneToastPacket(job.rootResult, job.rootAmount), player);
                }
                it.remove();
            }
        }
    }

    private static boolean ingredientsAvailable(Step step, List<PackagerBlockEntity> packagers) {
        for (BigItemStack ingredient : step.ingredients) {
            if (NetworkInventories.count(packagers, ingredient.stack) < ingredient.count) {
                return false;
            }
        }
        return true;
    }

    private static void ship(UUID networkId, Step step) {
        PackageOrder order = new PackageOrder(new ArrayList<>(step.ingredients));
        PackageOrderWithCrafts withCrafts = new PackageOrderWithCrafts(order, List.of());
        // Network-level request: routes from storage to the distributor's address without touching
        // any stock ticker's saved address.
        LogisticsManager.broadcastPackageRequest(networkId,
                LogisticallyLinkedBehaviour.RequestType.PLAYER, withCrafts, null, step.address);
    }

    private static String key(ItemStack stack) {
        return String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
