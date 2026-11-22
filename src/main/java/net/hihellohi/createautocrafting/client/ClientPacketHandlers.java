package net.hihellohi.createautocrafting.client;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.hihellohi.createautocrafting.client.screen.CraftPlanScreen;
import net.hihellohi.createautocrafting.manager.ClientPatternCache;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Client-only packet logic, kept in its own class so the dedicated server never loads the
 * {@link Minecraft} references inside. Only reached from the clientbound packet handlers.
 */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void openCraftPlan(BlockPos tickerPos, ItemStack result, int amount, CraftingPlan plan) {
        Minecraft.getInstance().setScreen(new CraftPlanScreen(tickerPos, result, amount, plan));
    }

    public static void showCraftDone(ItemStack result, int amount) {
        Minecraft.getInstance().getToasts().addToast(new CraftToast(result, amount));
    }

    public static void onPatternsSynced(UUID networkId, List<CraftingPattern> patterns) {
        ClientPatternCache.put(networkId, patterns);
        // If the request screen is open, have it rebuild so the new craftables show immediately.
        if (Minecraft.getInstance().screen instanceof StockKeeperRequestScreen screen) {
            screen.refreshSearchNextTick = true;
        }
    }
}
