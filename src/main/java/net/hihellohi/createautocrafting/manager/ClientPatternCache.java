package net.hihellohi.createautocrafting.manager;

import net.hihellohi.createautocrafting.pattern.CraftingPattern;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of the network's craftable patterns, populated by {@code PatternSyncPacket}.
 * This is what lets the stock-ticker craftable injection work on a dedicated server (where the
 * server-only {@link PatternRegistry} is not visible to the client). References only common types,
 * so it is safe to touch from either side.
 */
public final class ClientPatternCache {
    private static final Map<UUID, List<CraftingPattern>> CACHE = new ConcurrentHashMap<>();

    private ClientPatternCache() {}

    public static void put(UUID networkId, List<CraftingPattern> patterns) {
        if (networkId == null) {
            return;
        }
        if (patterns == null || patterns.isEmpty()) {
            CACHE.remove(networkId);
        } else {
            CACHE.put(networkId, List.copyOf(patterns));
        }
    }

    public static List<CraftingPattern> get(UUID networkId) {
        if (networkId == null) {
            return List.of();
        }
        return CACHE.getOrDefault(networkId, List.of());
    }
}
