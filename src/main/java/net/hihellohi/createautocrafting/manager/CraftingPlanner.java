package net.hihellohi.createautocrafting.manager;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Computes a {@link CraftingPlan} for "make {@code result × amount}" against a network: expands the
 * crafting tree, charges shared stock once (so two branches needing the same item don't both claim
 * it), and marks each branch satisfiable only when its raw leaves have enough network stock.
 */
public final class CraftingPlanner {
    private static final int MAX_DEPTH = 24;
    private static final int MAX_NODES = 256;

    private CraftingPlanner() {}

    public static CraftingPlan plan(ServerLevel level, UUID networkId, ItemStack result, int amount) {
        List<PackagerBlockEntity> packagers = NetworkInventories.packagers(level, networkId);
        Map<String, Integer> remaining = new HashMap<>();
        CraftingPlan.CraftNode root = build(level, networkId, packagers, remaining,
                result, Math.max(1, amount), new HashSet<>(), 0, new int[]{0});
        // Recursive: a craftable-but-unstocked ingredient is NOT "missing" as long as its own
        // ingredients (down to raw materials) are available — the job will craft it first.
        boolean satisfiable = root.satisfied;
        int stepsNeeded = countCraftSteps(root);
        int coilSteps = LogicCoilRegistry.craftingStepsOnNetwork(level, networkId);
        return new CraftingPlan(root, satisfiable, stepsNeeded, coilSteps);
    }

    /** One step per craftable node in the tree (each is a craft the player must perform). */
    private static int countCraftSteps(CraftingPlan.CraftNode node) {
        int steps = node.craftable ? 1 : 0;
        for (CraftingPlan.CraftNode child : node.children) {
            steps += countCraftSteps(child);
        }
        return steps;
    }

    private static CraftingPlan.CraftNode build(ServerLevel level, UUID networkId,
                                                List<PackagerBlockEntity> packagers,
                                                Map<String, Integer> remaining,
                                                ItemStack item, int amount,
                                                Set<String> inProgress, int depth, int[] nodeBudget) {
        String key = key(item);
        int total = NetworkInventories.count(packagers, item);
        int avail = remaining.computeIfAbsent(key, k -> total);
        int use = Math.min(avail, amount);
        remaining.put(key, avail - use);
        int toCraft = amount - use;

        CraftingPattern pattern = PatternRegistry.getPatternForResult(level, networkId, item);
        boolean craftable = pattern != null;
        List<CraftingPlan.CraftNode> children = new ArrayList<>();
        boolean satisfied;

        if (toCraft <= 0) {
            satisfied = true;
        } else if (pattern == null) {
            satisfied = false; // raw material the network is short on
        } else if (depth > MAX_DEPTH || nodeBudget[0] > MAX_NODES || inProgress.contains(key)) {
            satisfied = false; // too deep / cyclic — treat as not plannable
        } else {
            inProgress.add(key);
            int per = Math.max(1, pattern.outputCount());
            int crafts = (toCraft + per - 1) / per;
            boolean allChildren = true;
            // Combine duplicate ingredient slots into one node (e.g. "x4 planks", not four "x1").
            for (ItemStack ingredient : pattern.mergedIngredients()) {
                nodeBudget[0]++;
                CraftingPlan.CraftNode child = build(level, networkId, packagers, remaining,
                        ingredient, Math.max(1, ingredient.getCount()) * crafts,
                        inProgress, depth + 1, nodeBudget);
                children.add(child);
                allChildren &= child.satisfied;
            }
            inProgress.remove(key);
            satisfied = allChildren;
        }

        return new CraftingPlan.CraftNode(item.copyWithCount(1), amount, total, craftable, satisfied, children);
    }

    private static String key(ItemStack stack) {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
