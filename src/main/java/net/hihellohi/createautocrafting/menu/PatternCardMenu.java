package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.items.PatternCardItem;
import net.hihellohi.createautocrafting.items.PatternData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Editor menu for a {@link PatternCardItem}. The 3x3 grid and the result slot are <em>ghost</em>
 * slots: they hold a count-1 picture of whatever you click or drag over them and never take the real
 * item. The finished recipe is written back onto the held card when the menu closes.
 */
public class PatternCardMenu extends AbstractContainerMenu {

    // Slot layout, relative to the GUI's top-left (matches textures/gui/pattern_card.png).
    public static final int GRID_X = 38;
    public static final int GRID_Y = 28;
    public static final int GRID_PITCH = 20;
    public static final int RESULT_X = 131;
    public static final int RESULT_Y = 48;
    private static final int INV_X = 8;
    private static final int INV_Y = 109;

    /** 9 grid slots + 1 result slot come first in the slot list. */
    public static final int GHOST_SLOTS = PatternData.GRID_SIZE + 1;
    public static final int RESULT_SLOT = PatternData.GRID_SIZE;

    private final InteractionHand hand;
    private final ItemStack card;

    public final ItemStackHandler grid = new ItemStackHandler(PatternData.GRID_SIZE);
    public final ItemStackHandler result = new ItemStackHandler(1);
    /** While locked, nothing on the card can be edited. */
    public boolean locked;

    /** Client constructor: reads which hand holds the card. */
    public PatternCardMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readEnum(InteractionHand.class));
    }

    public PatternCardMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenus.PATTERN_CARD.get(), id);
        this.hand = hand;
        this.card = inv.player.getItemInHand(hand);

        PatternCardItem.readPattern(card, grid, result);
        this.locked = PatternCardItem.isLocked(card);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new GhostSlot(grid, col + row * 3, GRID_X + col * GRID_PITCH, GRID_Y + row * GRID_PITCH));
            }
        }
        addSlot(new GhostSlot(result, 0, RESULT_X, RESULT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    public static boolean isGhost(int slotIndex) {
        return slotIndex >= 0 && slotIndex < GHOST_SLOTS;
    }

    /** Sets a ghost slot to a count-1 copy of {@code stack} (empty clears it). Used by JEI drag-and-drop. */
    public void setGhost(int slotIndex, ItemStack stack) {
        if (locked || !isGhost(slotIndex)) {
            return;
        }
        slots.get(slotIndex).set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (!isGhost(slotId)) {
            super.clicked(slotId, dragType, clickType, player);
            return;
        }
        if (locked || clickType == ClickType.QUICK_CRAFT || clickType == ClickType.THROW) {
            return;
        }
        Slot slot = slots.get(slotId);
        if (clickType == ClickType.SWAP) {
            // Hovering a ghost slot + a hotbar key (0-8) or offhand (40) copies that item.
            ItemStack source = player.getInventory().getItem(dragType);
            slot.set(source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1));
            return;
        }
        ItemStack carried = getCarried();
        // Empty hand clears the slot; otherwise paint a copy. The carried stack is never consumed.
        slot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-clicking an inventory item copies it into the first free grid slot (nothing is moved).
        if (!locked && index >= GHOST_SLOTS) {
            ItemStack stack = slots.get(index).getItem();
            if (!stack.isEmpty()) {
                for (int i = 0; i < PatternData.GRID_SIZE; i++) {
                    if (grid.getStackInSlot(i).isEmpty()) {
                        grid.setStackInSlot(i, stack.copyWithCount(1));
                        break;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Button ids from the screen: {@code 0} toggles the lock; {@code id >= 1} sets a slot's count where
     * slot = {@code (id-1)/64} (0-8 grid, 9 result) and count = {@code ((id-1)%64)+1}.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            locked = !locked;
            return true;
        }
        if (locked) {
            return false;
        }
        int slot = (id - 1) / 64;
        int count = Mth.clamp(((id - 1) % 64) + 1, 1, 64);
        if (isGhost(slot)) {
            Slot target = slots.get(slot);
            ItemStack stack = target.getItem();
            if (!stack.isEmpty()) {
                target.set(stack.copyWithCount(count));
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof PatternCardItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof PatternCardItem) {
                PatternCardItem.writePattern(held, grid, result, locked);
            }
        }
    }

    /** A display-only slot: never accepts or gives items, but can show a stack of up to 64. */
    public static class GhostSlot extends SlotItemHandler {
        public GhostSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}
