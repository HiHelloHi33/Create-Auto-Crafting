package net.hihellohi.createautocrafting.event;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.CraftingDistributorBlock;
import net.hihellohi.createautocrafting.items.PatternCardItem;
import net.hihellohi.createautocrafting.manager.CraftJobManager;
import net.hihellohi.createautocrafting.manager.LogicCoilRegistry;
import net.hihellohi.createautocrafting.manager.PatternRegistry;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CreateAutoCrafting.MODID)
public final class ModEvents {
    /** Craft jobs advance once per second. */
    private static final int JOB_INTERVAL_TICKS = 20;
    private static int jobTicker;

    private ModEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++jobTicker >= JOB_INTERVAL_TICKS) {
            jobTicker = 0;
            CraftJobManager.tick(event.getServer());
        }
    }

    /**
     * Sneak + right-click a Crafting Distributor with a configured Pattern Card files the card straight
     * into it. (Vanilla skips a block's own use handler while sneaking with an item, hence the event.)
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        if (!player.isSecondaryUseActive()
                || !(event.getItemStack().getItem() instanceof PatternCardItem)
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CraftingDistributorBlock)) {
            return;
        }
        if (CraftingDistributorBlock.quickInsert(event.getLevel(), event.getPos(), player, event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        }
    }

    /** All of this mod's static state is per-server; drop it so nothing leaks into the next world. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PatternRegistry.clear();
        LogicCoilRegistry.clear();
        CraftJobManager.clear();
        jobTicker = 0;
    }
}
