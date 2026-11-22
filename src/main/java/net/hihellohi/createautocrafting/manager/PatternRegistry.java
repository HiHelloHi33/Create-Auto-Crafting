package net.hihellohi.createautocrafting.manager;

import net.hihellohi.createautocrafting.blocks.CraftingDistributorBlockEntity;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side record of the recipes each loaded Crafting Distributor offers, keyed by dimension +
 * position. A distributor's logistics network is resolved <em>live</em> at query time, so it can be
 * linked or re-linked at any moment and its recipes simply start or stop appearing on that network.
 *
 * <p>Distributors publish themselves when they load or their cards change, and remove themselves when
 * they unload or break, so this never holds on to a block that no longer exists. Clients learn about
 * recipes through {@code PatternSyncPacket} (see {@link ClientPatternCache}).
 */
public final class PatternRegistry {
    private static final Map<GlobalPos, List<CraftingPattern>> BY_PROVIDER = new ConcurrentHashMap<>();

    private PatternRegistry() {}

    public static void update(GlobalPos provider, List<CraftingPattern> patterns) {
        if (patterns.isEmpty()) {
            BY_PROVIDER.remove(provider);
        } else {
            BY_PROVIDER.put(provider, List.copyOf(patterns));
        }
    }

    public static void remove(GlobalPos provider) {
        BY_PROVIDER.remove(provider);
    }

    /** Forget everything (server stopped). */
    public static void clear() {
        BY_PROVIDER.clear();
    }

    /** Loaded distributors in this level that are linked to the given network and offer recipes. */
    public static List<CraftingDistributorBlockEntity> getProvidersForNetwork(ServerLevel level, UUID networkId) {
        List<CraftingDistributorBlockEntity> out = new ArrayList<>();
        if (networkId == null) {
            return out;
        }
        for (GlobalPos pos : BY_PROVIDER.keySet()) {
            if (!pos.dimension().equals(level.dimension())
                    || !level.isLoaded(pos.pos())
                    || !(level.getBlockEntity(pos.pos()) instanceof CraftingDistributorBlockEntity be)) {
                continue;
            }
            if (networkId.equals(be.getNetworkId())) {
                out.add(be);
            }
        }
        return out;
    }

    /** Every recipe offered on the network. */
    public static List<CraftingPattern> getPatternsForNetwork(ServerLevel level, UUID networkId) {
        List<CraftingPattern> out = new ArrayList<>();
        for (CraftingDistributorBlockEntity provider : getProvidersForNetwork(level, networkId)) {
            List<CraftingPattern> patterns = BY_PROVIDER.get(GlobalPos.of(level.dimension(), provider.getBlockPos()));
            if (patterns != null) {
                out.addAll(patterns);
            }
        }
        return out;
    }

    /** The recipe on this network that makes the given item, or null. */
    @Nullable
    public static CraftingPattern getPatternForResult(ServerLevel level, UUID networkId, ItemStack result) {
        for (CraftingPattern pattern : getPatternsForNetwork(level, networkId)) {
            if (ItemStack.isSameItemSameComponents(pattern.result(), result)) {
                return pattern;
            }
        }
        return null;
    }

    /** The distributor on this network that holds a recipe making the given item, or null. */
    @Nullable
    public static CraftingDistributorBlockEntity getProviderForResult(ServerLevel level, UUID networkId, ItemStack result) {
        for (CraftingDistributorBlockEntity provider : getProvidersForNetwork(level, networkId)) {
            List<CraftingPattern> patterns = BY_PROVIDER.get(GlobalPos.of(level.dimension(), provider.getBlockPos()));
            if (patterns == null) {
                continue;
            }
            for (CraftingPattern pattern : patterns) {
                if (ItemStack.isSameItemSameComponents(pattern.result(), result)) {
                    return provider;
                }
            }
        }
        return null;
    }
}
