package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.menu.PatternCardMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe card. Shift-right-click (in the air or on a block) opens an editor with a 3x3 recipe grid
 * and a result slot; the recipe is stored in the stack's {@link ModDataComponents#PATTERN} component.
 * Put a configured card into a Crafting Distributor to make its result craftable on the network.
 */
public class PatternCardItem extends Item {

    public PatternCardItem(Properties properties) {
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
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new PatternCardMenu(id, inv, hand),
                            Component.translatable("item.createautocrafting.pattern_card")),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isConfigured(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        CACTooltips.appendShiftInfo(tooltip,
                "tooltip.createautocrafting.pattern_card.1",
                "tooltip.createautocrafting.pattern_card.2");
        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ---- recipe persistence (DataComponent-backed) ---------------------------

    private static PatternData data(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PATTERN.get(), PatternData.EMPTY);
    }

    public static void readPattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result) {
        PatternData d = data(stack);
        for (int i = 0; i < PatternData.GRID_SIZE; i++) {
            grid.setStackInSlot(i, d.grid().get(i).copy());
        }
        result.setStackInSlot(0, d.result().copy());
    }

    public static void writePattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result, boolean locked) {
        List<ItemStack> g = new ArrayList<>(PatternData.GRID_SIZE);
        for (int i = 0; i < PatternData.GRID_SIZE; i++) {
            g.add(grid.getStackInSlot(i).copy());
        }
        stack.set(ModDataComponents.PATTERN.get(), new PatternData(g, result.getStackInSlot(0).copy(), locked));
    }

    /** Whether the card is locked against editing. Default false. */
    public static boolean isLocked(ItemStack stack) {
        return data(stack).locked();
    }

    public static boolean isConfigured(ItemStack stack) {
        return stack.getItem() instanceof PatternCardItem && !getResult(stack).isEmpty();
    }

    /** The recipe output, or empty if unconfigured. */
    public static ItemStack getResult(ItemStack stack) {
        return data(stack).result();
    }

    /** The nine recipe ingredients (some may be empty), in grid order. */
    public static List<ItemStack> getIngredients(ItemStack stack) {
        return data(stack).grid();
    }
}
