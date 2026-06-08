package net.hihellohi.createautocrafting.items;

import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The "pattern" of this mod. Shift-right-click (in hand, on air or a block) opens a filter-style
 * editor with a 3x3 recipe grid and a result slot. The configured recipe is stored in the stack's
 * NBT and later read by Pattern Providers so they can emit the ingredients and request the result.
 */
public class CraftingBlueprintItem extends Item implements MenuProvider {
    private static final String TAG = "Blueprint";
    private static final String TAG_GRID = "Grid";
    private static final String TAG_RESULT = "Result";
    private static final String TAG_LOCKED = "Locked";

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
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeEnum(hand));
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

    // ---- pattern persistence -------------------------------------------------

    public static void readPattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null) {
            return;
        }
        if (tag.contains(TAG_GRID)) {
            grid.deserializeNBT(tag.getCompound(TAG_GRID));
        }
        if (tag.contains(TAG_RESULT)) {
            result.deserializeNBT(tag.getCompound(TAG_RESULT));
        }
    }

    public static void writePattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result) {
        writePattern(stack, grid, result, isLocked(stack));
    }

    public static void writePattern(ItemStack stack, ItemStackHandler grid, ItemStackHandler result, boolean locked) {
        CompoundTag tag = new CompoundTag();
        tag.put(TAG_GRID, grid.serializeNBT());
        tag.put(TAG_RESULT, result.serializeNBT());
        tag.putBoolean(TAG_LOCKED, locked);
        stack.getOrCreateTag().put(TAG, tag);
    }

    /** Whether the blueprint is locked from editing. Default false (unlocked = fully editable);
     *  saved per blueprint stack so each remembers its own state once toggled. */
    public static boolean isLocked(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag != null && tag.getBoolean(TAG_LOCKED);
    }

    public static boolean isConfigured(ItemStack stack) {
        return !getResult(stack).isEmpty();
    }

    /** The recipe output this blueprint produces, or empty if unconfigured. */
    public static ItemStack getResult(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null || !tag.contains(TAG_RESULT)) {
            return ItemStack.EMPTY;
        }
        ItemStackHandler result = new ItemStackHandler(1);
        result.deserializeNBT(tag.getCompound(TAG_RESULT));
        return result.getStackInSlot(0);
    }

    /** The nine recipe ingredients (some may be empty), in grid order. */
    public static List<ItemStack> getIngredients(ItemStack stack) {
        ItemStackHandler grid = new ItemStackHandler(9);
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag != null && tag.contains(TAG_GRID)) {
            grid.deserializeNBT(tag.getCompound(TAG_GRID));
        }
        return java.util.stream.IntStream.range(0, 9)
                .mapToObj(grid::getStackInSlot)
                .toList();
    }
}
