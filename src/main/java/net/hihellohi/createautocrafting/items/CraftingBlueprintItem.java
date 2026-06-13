package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The "pattern" of this mod. Shift-right-click (in hand, on air or a block) opens a filter-style
 * editor with a 3x3 recipe grid and a result slot. The configured recipe is stored in the stack's
 * {@link ModDataComponents#BLUEPRINT} component and later read by Pattern Providers so they can
 * emit the ingredients and request the result.
 */
public class CraftingBlueprintItem extends Item implements MenuProvider {

    public CraftingBlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            return new InteractionResultHolder<>(openEditor(level, player, hand), stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            return openEditor(context.getLevel(), player, context.getHand());
        }
        return InteractionResult.PASS;
    }

    private InteractionResult openEditor(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new CraftingBlueprintMenu(id, inv, hand),
                    getName(player.getItemInHand(hand)));
            serverPlayer.openMenu(provider, buf -> buf.writeEnum(hand));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("item.createautocrafting.crafting_blueprint");
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inv, Player player) {
        // Real opening goes through openEditor(); this satisfies MenuProvider for completeness.
        return null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isConfigured(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        CACTooltips.appendShiftInfo(tooltip,
                "tooltip.createautocrafting.crafting_blueprint.1",
                "tooltip.createautocrafting.crafting_blueprint.2");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ---- pattern persistence (DataComponent-backed) --------------------------

    private static BlueprintData data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), BlueprintData.EMPTY);
    }

    public static void readPattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result) {
        BlueprintData d = data(stack);
        for (int i = 0; i < 9; i++) {
            grid.setStackInSlot(i, d.grid().get(i).copy());
        }
        result.setStackInSlot(0, d.result().copy());
    }

    public static void writePattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result) {
        writePattern(stack, grid, result, isLocked(stack));
    }

    public static void writePattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result, boolean locked) {
        List<ItemStack> g = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            g.add(grid.getStackInSlot(i).copy());
        }
        stack.set(ModDataComponents.BLUEPRINT.get(), new BlueprintData(g, result.getStackInSlot(0).copy(), locked));
    }

    /** Whether the blueprint is locked from editing. Default false (unlocked = fully editable). */
    public static boolean isLocked(ItemStack stack) {
        return data(stack).locked();
    }

    public static boolean isConfigured(ItemStack stack) {
        return !getResult(stack).isEmpty();
    }

    /** The recipe output this blueprint produces, or empty if unconfigured. */
    public static ItemStack getResult(ItemStack stack) {
        return data(stack).result();
    }

    /** The nine recipe ingredients (some may be empty), in grid order. */
    public static List<ItemStack> getIngredients(ItemStack stack) {
        return data(stack).grid();
    }
}
