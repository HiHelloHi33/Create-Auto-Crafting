package net.hihellohi.createautocrafting.integration;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.manager.VirtualCraftingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Bridges Logic Coils / PRVs into Create's logistics layer via virtual stock entries.
 */
public final class CreateLogisticsBridge {
    private CreateLogisticsBridge() {}

    public static VirtualStockLink registerRequest(ServerLevel level, BlockPos packagerPos,
                                                     ItemStack item, int amount, UUID playerId) {
        UUID networkId = PatternProviderHelper.getNetworkId(level, packagerPos);
        if (networkId == null) {
            return null;
        }
        return VirtualCraftingManager.registerVirtualStock(networkId, packagerPos, item, amount, playerId);
    }

    public static void mergeVirtualStock(UUID networkId, InventorySummary summary) {
        for (VirtualStockLink link : VirtualCraftingManager.getStocksForNetwork(networkId)) {
            link.mergeInto(summary);
        }
    }

    public static int getVirtualStockCount(UUID networkId, ItemStack stack) {
        int total = 0;
        for (VirtualStockLink link : VirtualCraftingManager.getStocksForNetwork(networkId)) {
            if (ItemStack.isSameItemSameComponents(link.getItem(), stack)) {
                total += Math.max(0, link.getTargetAmount() - link.getCurrentAmount());
            }
        }
        return total;
    }

    public static void invalidateNetworkCache(UUID networkId) {
        LogisticsManager.SUMMARIES.invalidate(networkId);
        LogisticsManager.ACCURATE_SUMMARIES.invalidate(networkId);
    }

    public static boolean isPatternProviderPackager(PackagerBlockEntity packager) {
        return PatternProviderHelper.isPatternProvider(packager);
    }
}
