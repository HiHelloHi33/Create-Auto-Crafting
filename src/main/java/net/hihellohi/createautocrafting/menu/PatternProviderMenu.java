package net.hihellohi.createautocrafting.menu;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.hihellohi.createautocrafting.items.CraftingBlueprintItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

/**
 * The Pattern Provider's screen, in the spirit of the frogport UI (a flat panel of slots) but with
 * none of the frogport address/shipping behaviour. It is a small inventory of <em>blueprint</em>
 * items: dropping a configured Crafting Blueprint into a slot teaches the provider that recipe;
 * taking it back out removes it. Real slots are used so vanilla handles the display and item
 * movement; contents are mirrored to the underlying Packager block entity on every change.
 */
public class PatternProviderMenu extends AbstractContainerMenu {

    // Two rows of eight pattern slots, laid out to match crafting_manager.png.
    public static final int PATTERN_COLS = 8;
    public static final int PATTERN_ROWS = 2;
    public static final int PATTERN_SLOTS = PATTERN_COLS * PATTERN_ROWS;
    private static final int SLOT_X = 16;
    private static final int SLOT_Y = 27;
    private static final int SLOT_PITCH_X = 19;
    private static final int SLOT_PITCH_Y = 19;
    private static final int INV_X = 11;
    private static final int INV_Y = 99;

    private final Player player;
    private final BlockPos providerPos;
    private final Level level;
    public final ItemStackHandler patterns = new ItemStackHandler(PATTERN_SLOTS);

    public PatternProviderMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public PatternProviderMenu(int id, Inventory inv, BlockPos providerPos) {
        super(ModMenus.PATTERN_PROVIDER.get(), id);
        this.player = inv.player;
        this.level = inv.player.level();
        this.providerPos = providerPos;

        PackagerBlockEntity packager = getProvider();
        if (packager != null) {
            List<ItemStack> stored = PatternProviderHelper.getPatternStacks(packager);
            for (int i = 0; i < PATTERN_SLOTS && i < stored.size(); i++) {
                patterns.setStackInSlot(i, stored.get(i));
            }
        }

        for (int i = 0; i < PATTERN_SLOTS; i++) {
            int col = i % PATTERN_COLS;
            int row = i / PATTERN_COLS;
            addSlot(new PatternSlot(patterns, i, SLOT_X + col * SLOT_PITCH_X, SLOT_Y + row * SLOT_PITCH_Y));
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

    private PackagerBlockEntity getProvider() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(providerPos);
        if (be instanceof PackagerBlockEntity packager && PatternProviderHelper.isPatternProvider(packager)) {
            return packager;
        }
        return null;
    }

    /** Persist slot contents to the block entity and republish to the network registry. */
    private void persist() {
        if (level == null || level.isClientSide) {
            return;
        }
        PackagerBlockEntity packager = getProvider();
        if (packager == null) {
            return;
        }
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int i = 0; i < PATTERN_SLOTS; i++) {
            list.add(patterns.getStackInSlot(i));
        }
        PatternProviderHelper.setPatterns(packager, list);
        PatternProviderHelper.refreshRegistry(level, providerPos, packager);
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        persist();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        persist();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < PATTERN_SLOTS) {
            // pattern slot -> player inventory
            if (!moveItemStackTo(stack, PATTERN_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof CraftingBlueprintItem) {
            // player inventory -> pattern slots
            if (!moveItemStackTo(stack, 0, PATTERN_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return getProvider() != null
                && player.distanceToSqr(providerPos.getX() + 0.5, providerPos.getY() + 0.5,
                providerPos.getZ() + 0.5) < 64;
    }

    /** Only accepts configured Crafting Blueprints, one per slot. */
    public static class PatternSlot extends SlotItemHandler {
        public PatternSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CraftingBlueprintItem && CraftingBlueprintItem.isConfigured(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
