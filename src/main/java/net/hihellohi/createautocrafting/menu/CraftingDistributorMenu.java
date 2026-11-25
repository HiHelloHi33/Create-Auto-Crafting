package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.blocks.CraftingDistributorBlockEntity;
import net.hihellohi.createautocrafting.items.PatternCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Crafting Distributor's screen: two rows of eight Pattern Card slots above the player inventory.
 * On the server the slots are the block entity's own handler, so there is nothing to copy or save.
 */
public class CraftingDistributorMenu extends AbstractContainerMenu {

    public static final int PATTERN_COLS = 8;
    public static final int PATTERN_SLOTS = CraftingDistributorBlockEntity.PATTERN_SLOTS;

    // Slot layout relative to the GUI's top-left (matches textures/gui/crafting_distributor.png).
    private static final int SLOT_X = 16;
    private static final int SLOT_Y = 28;
    private static final int SLOT_PITCH = 19;
    private static final int INV_X = 11;
    private static final int INV_Y = 99;

    private final BlockPos pos;

    /** Client constructor. */
    public CraftingDistributorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(PATTERN_SLOTS));
    }

    /** Server constructor. */
    public CraftingDistributorMenu(int id, Inventory inv, CraftingDistributorBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.patterns);
    }

    private CraftingDistributorMenu(int id, Inventory inv, BlockPos pos, IItemHandler patterns) {
        super(ModMenus.CRAFTING_DISTRIBUTOR.get(), id);
        this.pos = pos;

        for (int i = 0; i < PATTERN_SLOTS; i++) {
            addSlot(new PatternSlot(patterns, i,
                    SLOT_X + (i % PATTERN_COLS) * SLOT_PITCH, SLOT_Y + (i / PATTERN_COLS) * SLOT_PITCH));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        boolean moved = index < PATTERN_SLOTS
                ? moveItemStackTo(stack, PATTERN_SLOTS, slots.size(), true)       // card -> inventory
                : PatternCardItem.isConfigured(stack)
                && moveItemStackTo(stack, 0, PATTERN_SLOTS, false);                // inventory -> cards
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockEntity be = player.level().getBlockEntity(pos);
        return be instanceof CraftingDistributorBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }

    /** Only accepts configured Pattern Cards, one per slot. */
    public static class PatternSlot extends SlotItemHandler {
        public PatternSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PatternCardItem.isConfigured(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
