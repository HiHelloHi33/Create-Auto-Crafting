package net.hihellohi.createautocrafting.manager;

import net.hihellohi.createautocrafting.integration.VirtualStockLink;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class VirtualCraftingManager {
    private static final Map<UUID, List<VirtualStockLink>> BY_NETWORK = new ConcurrentHashMap<>();
    private static final Map<BlockPos, VirtualStockLink> BY_PACKAGER = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<BlockPos>> PLAYER_PACKAGERS = new ConcurrentHashMap<>();

    private VirtualCraftingManager() {}

    public static VirtualStockLink registerVirtualStock(UUID networkId, BlockPos packagerPos,
                                                        ItemStack item, int amount, UUID playerUUID) {
        VirtualStockLink stock = new VirtualStockLink(networkId, item, amount, playerUUID);
        BY_PACKAGER.put(packagerPos.immutable(), stock);
        BY_NETWORK.computeIfAbsent(networkId, id -> Collections.synchronizedList(new ArrayList<>())).add(stock);
        PLAYER_PACKAGERS.computeIfAbsent(playerUUID, id -> ConcurrentHashMap.newKeySet()).add(packagerPos.immutable());
        return stock;
    }

    public static VirtualStockLink getVirtualStock(BlockPos packagerPos) {
        return BY_PACKAGER.get(packagerPos);
    }

    public static List<VirtualStockLink> getStocksForNetwork(UUID networkId) {
        List<VirtualStockLink> stocks = BY_NETWORK.get(networkId);
        return stocks == null ? List.of() : List.copyOf(stocks);
    }

    public static void recordDelivery(BlockPos packagerPos, int count) {
        VirtualStockLink stock = BY_PACKAGER.get(packagerPos);
        if (stock != null) {
            stock.addDelivered(count);
        }
    }

    public static void removeVirtualStock(BlockPos packagerPos) {
        VirtualStockLink stock = BY_PACKAGER.remove(packagerPos);
        if (stock == null) {
            return;
        }
        List<VirtualStockLink> networkList = BY_NETWORK.get(stock.getNetworkId());
        if (networkList != null) {
            networkList.remove(stock);
        }
        Set<BlockPos> packagers = PLAYER_PACKAGERS.get(stock.getRequestingPlayer());
        if (packagers != null) {
            packagers.remove(packagerPos);
        }
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        BY_PACKAGER.entrySet().removeIf(entry -> {
            VirtualStockLink stock = entry.getValue();
            boolean stale = now - stock.getCreatedAt() > 3_600_000L && !stock.isFulfilled();
            if (stale) {
                removeVirtualStock(entry.getKey());
            }
            return stale;
        });
    }
}
