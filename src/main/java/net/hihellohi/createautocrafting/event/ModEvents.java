package net.hihellohi.createautocrafting.event;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.items.CraftingBlueprintItem;
import net.hihellohi.createautocrafting.items.PackagerCrafterKitItem;
import net.hihellohi.createautocrafting.manager.VirtualCraftingManager;
import net.hihellohi.createautocrafting.menu.PatternProviderMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CreateAutoCrafting.MODID)
public class ModEvents {
    private static int cleanupTicker;

    private static int pollTicker;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++cleanupTicker >= 1200) {
            cleanupTicker = 0;
            VirtualCraftingManager.cleanup();
        }
        // Advance multi-step craft jobs once per second (ship ready steps, detect completion).
        if (++pollTicker >= 20) {
            pollTicker = 0;
            net.hihellohi.createautocrafting.manager.CraftJobManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onRightClickPatternProvider(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PackagerBlockEntity packager)) {
            return;
        }

        ItemStack held = event.getItemStack();
        boolean sneaking = player.isSecondaryUseActive();

        // --- Conversion: kit on a normal Packager turns it into a Pattern Provider --------------
        if (!PatternProviderHelper.isPatternProvider(packager)) {
            if (!(held.getItem() instanceof PackagerCrafterKitItem)) {
                return; // ordinary packager + non-kit: let Create handle it normally
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (level.isClientSide) {
                return;
            }
            PatternProviderHelper.markPatternProvider(packager, player.getUUID(), player.getName().getString());
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
            if (!player.isCreative()) {
                held.shrink(1);
            }
            player.displayClientMessage(
                    Component.translatable("message.createautocrafting.converted"), true);
            return;
        }

        boolean quickInsert = sneaking && held.getItem() instanceof CraftingBlueprintItem;

        // When sneaking with anything other than a blueprint, do NOT intercept: this lets the
        // player shift-place a Stock Link (or any block) directly onto the Pattern Provider.
        if (sneaking && !quickInsert) {
            return;
        }

        // Non-sneak opens our GUI; sneak+blueprint quick-inserts. Either way we own the click so the
        // underlying Packager UI never opens for a PRV.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) {
            return;
        }

        if (quickInsert) {
            if (!CraftingBlueprintItem.isConfigured(held)) {
                return;
            }
            if (PatternProviderHelper.getPatternCount(packager) >= PatternProviderMenu.PATTERN_SLOTS) {
                return;
            }
            PatternProviderHelper.addPattern(packager, held);
            PatternProviderHelper.refreshRegistry(level, pos, packager);
            if (!player.isCreative()) {
                held.shrink(1);
            }
            return;
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new PatternProviderMenu(id, inv, pos),
                            Component.translatable("block.createautocrafting.pattern_provider")),
                    buf -> buf.writeBlockPos(pos));
        }
    }
}
