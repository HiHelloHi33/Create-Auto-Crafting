package net.hihellohi.createautocrafting.client;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.hihellohi.createautocrafting.blocks.LogicCoilBlockEntity;
import net.hihellohi.createautocrafting.client.screen.CraftPlanScreen;
import net.hihellohi.createautocrafting.manager.ClientPatternCache;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.UUID;

/**
 * Client-only packet logic, kept in its own class so the dedicated server never classloads the
 * {@link Minecraft}/{@code ClientLevel} references inside. Reached exclusively through
 * {@code DistExecutor} from the (common) packet {@code handle} methods.
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

    public static void virtualStockSync(BlockPos pos, ItemStack displayItem, int current, int target) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LogicCoilBlockEntity coil) {
            coil.updateClientDisplay(displayItem, current, target);
        }
    }

    public static void patternCompleted(BlockPos pos, ItemStack resultItem, int amount) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        // Hook for client-side completion feedback (particles/sound) on the Logic Coil.
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LogicCoilBlockEntity) {
            level.playLocalSound(pos, net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.6f, false);
        }
    }
}
