package net.hihellohi.createautocrafting.manager;

import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side record of which crafting patterns each Pattern Provider exposes, keyed by provider
 * position + dimension. The provider's logistics network is resolved <em>live</em> at query time
 * (from the world), so a provider can be linked or re-linked at any point after its patterns are
 * set — the patterns simply start/stop appearing on whatever network it currently belongs to.
 *
 * <p>This is server authoritative; clients learn about patterns through {@code PatternSyncPacket}
 * (see {@link ClientPatternCache}), which is what makes the feature work on dedicated servers.
 */
public final class PatternRegistry {

    private record Entry(ResourceKey<Level> dimension, List<CraftingPattern> patterns) {}

    private static final Map<BlockPos, Entry> BY_PROVIDER = new ConcurrentHashMap<>();

    private PatternRegistry() {}

    public static void update(BlockPos providerPos, ResourceKey<Level> dimension, List<CraftingPattern> patterns) {
        BlockPos key = providerPos.immutable();
        if (dimension == null || patterns == null || patterns.isEmpty()) {
            BY_PROVIDER.remove(key);
            return;
        }
        BY_PROVIDER.put(key, new Entry(dimension, List.copyOf(patterns)));
    }

    public static void remove(BlockPos providerPos) {
        BY_PROVIDER.remove(providerPos.immutable());
    }

    /** Every pattern whose provider currently resolves to the given network in this level. */
    public static List<CraftingPattern> getPatternsForNetwork(ServerLevel level, UUID networkId) {
        List<CraftingPattern> out = new ArrayList<>();
        if (networkId == null) {
            return out;
        }
        for (Map.Entry<BlockPos, Entry> e : BY_PROVIDER.entrySet()) {
            if (!e.getValue().dimension().equals(level.dimension())) {
                continue;
            }
            if (networkId.equals(PatternProviderHelper.getNetworkId(level, e.getKey()))) {
                out.addAll(e.getValue().patterns());
            }
        }
        return out;
    }

    /** The pattern on this network whose result matches the given stack, or null. */
    public static net.hihellohi.createautocrafting.pattern.CraftingPattern getPatternForResult(
            ServerLevel level, UUID networkId, net.minecraft.world.item.ItemStack result) {
        for (CraftingPattern pattern : getPatternsForNetwork(level, networkId)) {
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(pattern.result(), result)) {
                return pattern;
            }
        }
        return null;
    }

    /** The provider position on this network that exposes a pattern producing the given stack. */
    public static BlockPos getProviderForResult(ServerLevel level, UUID networkId,
                                                net.minecraft.world.item.ItemStack result) {
        for (BlockPos pos : getProvidersForNetwork(level, networkId)) {
            Entry entry = BY_PROVIDER.get(pos);
            if (entry == null) {
                continue;
            }
            for (CraftingPattern pattern : entry.patterns()) {
                if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(pattern.result(), result)) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** Provider positions that currently resolve to the given network in this level. */
    public static List<BlockPos> getProvidersForNetwork(ServerLevel level, UUID networkId) {
        List<BlockPos> out = new ArrayList<>();
        if (networkId == null) {
            return out;
        }
        for (Map.Entry<BlockPos, Entry> e : BY_PROVIDER.entrySet()) {
            if (!e.getValue().dimension().equals(level.dimension())) {
                continue;
            }
            if (networkId.equals(PatternProviderHelper.getNetworkId(level, e.getKey()))) {
                out.add(e.getKey());
            }
        }
        return out;
    }
}
