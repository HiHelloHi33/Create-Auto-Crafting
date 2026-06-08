package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.items.CraftingBlueprintItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Filter-style configuration menu for a {@link CraftingBlueprintItem}. The 3x3 grid and the
 * single result slot are <em>ghost</em> slots — they hold a visual copy (count 1) of whatever the
 * player clicks with, never the real items. The layout mirrors Create's filter menus: the
 * recipe-in grid on the left, the recipe-out slot on the right.
 *
 * <p>The configured pattern is serialised back onto the held blueprint stack when the menu closes.
 */
public class CraftingBlueprintMenu extends AbstractContainerMenu {

    // Slot layout (relative to the GUI's top-left), matched to the crafting_blueprint.png texture.
    public static final int GRID_X = 38;
    public static final int GRID_Y = 28;
    public static final int GRID_PITCH = 20;
    public static final int RESULT_X = 131;
    public static final int RESULT_Y = 48;
    // Player inventory area (matches the wells drawn in crafting_blueprint.png).
    private static final int INV_X = 8;
    private static final int INV_Y = 109;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack blueprint;

    public final ItemStackHandler grid = new ItemStackHandler(9);
    public final ItemStackHandler result = new ItemStackHandler(1);
    /** When locked, nothing can be edited (items or amounts). Default unlocked = fully editable. */
    public boolean locked = false;

    /** Client/network constructor — reads which hand holds the blueprint. */
    public CraftingBlueprintMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readEnum(InteractionHand.class));
    }

    public CraftingBlueprintMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenus.CRAFTING_BLUEPRINT.get(), id);
        this.player = inv.player;
        this.hand = hand;
        this.blueprint = player.getItemInHand(hand);

        CraftingBlueprintItem.readPattern(blueprint, grid, result);
        this.locked = CraftingBlueprintItem.isLocked(blueprint);

        // Recipe-in grid (ghost) — amounts editable (1..64) when unlocked.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new GhostSlot(grid, index, GRID_X + col * GRID_PITCH, GRID_Y + row * GRID_PITCH, 64));
            }
        }
        // Recipe-out (ghost) — allows an output count > 1 (set by scrolling the slot).
        addSlot(new GhostSlot(result, 0, RESULT_X, RESULT_Y, 64));

        // Player inventory + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
    }

    /** Sets a ghost slot (0..8 grid, 9 result) to a count-1 copy of {@code stack} (empty clears it).
     *  Used by the JEI ghost-ingredient drag path (server-side). */
    public void setGhost(int slotIndex, ItemStack stack) {
        if (locked || slotIndex < 0 || slotIndex >= GHOST_SLOTS) {
            return;
        }
        slots.get(slotIndex).set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    private static final int GHOST_SLOTS = 10; // 9 grid + 1 result

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GHOST_SLOTS) {
            if (locked) {
                return; // locked: no item edits at all
            }
            Slot slot = slots.get(slotId);
            if (clickType == ClickType.SWAP) {
                // Hovering a ghost slot + a hotbar key (dragType 0-8) or offhand (40) copies that item.
                ItemStack source = player.getInventory().getItem(dragType);
                slot.set(source.isEmpty() ? ItemStack.EMPTY : source.copyWithCount(1));
                return;
            }
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY); // click with empty hand clears the ghost
            } else {
                slot.set(carried.copyWithCount(1));
            }
            return; // never consume the carried stack
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Ghost slots can't receive shift-clicked items; shift-click just shuffles inv <-> hotbar.
        if (index < GHOST_SLOTS) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int invStart = GHOST_SLOTS;
        int invEnd = slots.size();
        int hotbarStart = invEnd - 9;
        boolean inHotbar = index >= hotbarStart;

        int destStart = inHotbar ? invStart : hotbarStart;
        int destEnd = inHotbar ? hotbarStart : invEnd;
        if (!moveItemStackTo(stack, destStart, destEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /**
     * Encoded actions from the screen:
     * <ul><li>{@code id == 0}: toggle the lock (always allowed).</li>
     * <li>{@code id >= 1}: set a slot's count, where slot = {@code (id-1)/64} (0..8 grid, 9 result)
     * and count = {@code ((id-1)%64)+1}. All count edits are ignored while locked.</li></ul>
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            locked = !locked;
            return true;
        }
        if (locked) {
            return false; // locked: no amount edits (grid or result)
        }
        int slot = (id - 1) / 64;
        int count = net.minecraft.util.Mth.clamp(((id - 1) % 64) + 1, 1, 64);
        if (slot >= 0 && slot < 9) {
            ItemStack stack = grid.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.setCount(count);
                grid.setStackInSlot(slot, stack);
            }
        } else if (slot == 9) {
            ItemStack out = result.getStackInSlot(0);
            if (!out.isEmpty()) {
                out.setCount(count);
                result.setStackInSlot(0, out);
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof CraftingBlueprintItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            CraftingBlueprintItem.writePattern(blueprint, grid, result, locked);
        }
    }

    /** A non-interactive display slot. Grid slots hold count-1 visuals; the result slot allows the
     *  configured output count so it can show "how many the recipe makes". */
    public static class GhostSlot extends SlotItemHandler {
        private final int maxStack;

        public GhostSlot(IItemHandler handler, int index, int x, int y) {
            this(handler, index, x, y, 1);
        }

        public GhostSlot(IItemHandler handler, int index, int x, int y, int maxStack) {
            super(handler, index, x, y);
            this.maxStack = maxStack;
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
            return maxStack;
        }
    }
}
