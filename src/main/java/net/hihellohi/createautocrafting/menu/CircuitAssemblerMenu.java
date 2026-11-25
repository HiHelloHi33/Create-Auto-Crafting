package net.hihellohi.createautocrafting.menu;

import net.hihellohi.createautocrafting.blocks.CircuitAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Circuit Assembler's screen: the plate slot in the middle, the five component slots around it,
 * the output slot to the right and the player inventory below. Slots are the block entity's own handler.
 */
public class CircuitAssemblerMenu extends AbstractContainerMenu {
    /** Slot item positions (top-left of the 16x16 item) matching textures/gui/circuit_assembler.png. */
    public static final int PLATE_X = 59;
    public static final int PLATE_Y = 64;
    public static final int OUTPUT_X = 151;
    public static final int OUTPUT_Y = 64;
    /** Component seats in COMPONENT_ITEMS order: couch left, couch right, right armchair, bottom, left armchair. */
    public static final int[][] COMPONENT_POS = {{37, 26}, {81, 26}, {98, 60}, {59, 95}, {20, 60}};
    public static final int INV_X = 8;
    public static final int INV_Y = 138;

    /** Button ids for {@link #clickMenuButton}. */
    public static final int BUTTON_TOGGLE = 0;
    public static final int BUTTON_TIER_UP = 1;
    public static final int BUTTON_TIER_DOWN = 2;

    private static final int MACHINE_SLOTS = CircuitAssemblerBlockEntity.SLOTS;

    private final BlockPos pos;
    private final ContainerData data;

    public CircuitAssemblerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos(), new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(4));
    }

    public CircuitAssemblerMenu(int id, Inventory inv, CircuitAssemblerBlockEntity be) {
        this(id, inv, be.getBlockPos(), be.inventory, be.data);
    }

    private CircuitAssemblerMenu(int id, Inventory inv, BlockPos pos, IItemHandler handler, ContainerData data) {
        super(ModMenus.CIRCUIT_ASSEMBLER.get(), id);
        this.pos = pos;
        this.data = data;

        addSlot(new SlotItemHandler(handler, CircuitAssemblerBlockEntity.SLOT_PLATE, PLATE_X, PLATE_Y));
        for (int i = 0; i < CircuitAssemblerBlockEntity.COMPONENT_COUNT; i++) {
            addSlot(new SlotItemHandler(handler, CircuitAssemblerBlockEntity.FIRST_COMPONENT + i,
                    COMPONENT_POS[i][0], COMPONENT_POS[i][1]));
        }
        addSlot(new SlotItemHandler(handler, CircuitAssemblerBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    /** Progress of the running craft, 0..1. */
    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0 : Math.min(1f, data.get(0) / (float) total);
    }

    /** Whether the machine is switched on (green) rather than paused (red). */
    public boolean isActive() {
        return data.get(3) != 0;
    }

    /** Selected circuit tier, 0-based (0 = tier one). */
    public int tier() {
        return data.get(2);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof CircuitAssemblerBlockEntity assembler)) {
            return false;
        }
        switch (id) {
            case BUTTON_TOGGLE -> assembler.toggleActive();
            case BUTTON_TIER_UP -> assembler.cycleTier(1);
            case BUTTON_TIER_DOWN -> assembler.cycleTier(-1);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        boolean moved = index < MACHINE_SLOTS
                ? moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, MACHINE_SLOTS - 1, false);
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
        return be instanceof CircuitAssemblerBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
}
