package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.etching.AssemblyRecipe;
import net.hihellohi.createautocrafting.etching.AssemblyRecipeInput;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.hihellohi.createautocrafting.menu.CircuitAssemblerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Circuit Assembler's brain (controller corner). Slot 0 is the etched plate, slots 1-5 are the
 * component feeders (one per component type) and slot 6 is the output. While rotation is applied it
 * runs the best matching {@link AssemblyRecipe}: more components means a higher tier circuit.
 */
public class CircuitAssemblerBlockEntity extends FactoryControllerBlockEntity {
    public static final int SLOT_PLATE = 0;
    public static final int FIRST_COMPONENT = 1;
    public static final int COMPONENT_COUNT = 5;
    public static final int SLOT_OUTPUT = 6;
    public static final int SLOTS = 7;

    /** The circuit each tier setting makes, index 0 = tier one. */
    public static final List<Supplier<Item>> TIER_CIRCUITS = List.of(
            ModItems.T1_CIRCUIT, ModItems.T2_CIRCUIT, ModItems.T3_CIRCUIT);

    /** Which item each component slot takes, in slot order 1..5 (also the order of the feeder rack). */
    public static final List<Supplier<Item>> COMPONENT_ITEMS = List.of(
            ModItems.RESISTOR, ModItems.INDUCTOR, ModItems.DIODE, ModItems.CAPACITOR, ModItems.TRANSISTOR);

    public final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            recipeDirty = true;
            setChanged();
            if (level != null && !level.isClientSide) {
                sendData();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_PLATE) {
                return stack.is(ModItems.T1_ETCHED_PLATE.get()) || stack.is(ModItems.T2_ETCHED_PLATE.get());
            }
            if (slot >= FIRST_COMPONENT && slot < SLOT_OUTPUT) {
                return stack.is(COMPONENT_ITEMS.get(slot - FIRST_COMPONENT).get());
            }
            return false;
        }
    };

    /** Hoppers and belts: insert anything that fits the matching slot, extract only from the output. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_OUTPUT ? stack : inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUTPUT && inventory.isItemValid(slot, stack);
        }
    };

    /** Progress and total work, for the screen's progress bar. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> workNeeded;
                case 2 -> tier;
                default -> active ? 1 : 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> workNeeded = value;
                case 2 -> tier = value;
                default -> active = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    /** Which circuit tier to make (0-based index into {@link #TIER_CIRCUITS}). */
    private int tier;
    /** The tier of the craft in progress; a tier change only takes effect once that craft finishes. */
    private int craftTier;
    /** Green (true) runs the machine, red (false) pauses it. Starts on. */
    private boolean active = true;

    private boolean recipeDirty = true;
    private RecipeHolder<AssemblyRecipe> cachedRecipe;
    private int[] cachedSlots;

    public CircuitAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CIRCUIT_ASSEMBLER_BE.get(), pos, state);
        workNeeded = AssemblyRecipe.DEFAULT_WORK;
    }

    public boolean isActive() {
        return active;
    }

    public int getTier() {
        return tier;
    }

    public void toggleActive() {
        active = !active;
        setChanged();
        sendData();
    }

    public void cycleTier(int delta) {
        int n = TIER_CIRCUITS.size();
        tier = ((tier + delta) % n + n) % n;
        setChanged();
        sendData();
    }

    @Override
    public void onFormed() {
        active = true;
        super.onFormed();
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    private AssemblyRecipeInput input() {
        List<ItemStack> stacks = new ArrayList<>(SLOTS - 1);
        for (int i = 0; i < SLOT_OUTPUT; i++) {
            stacks.add(inventory.getStackInSlot(i));
        }
        return new AssemblyRecipeInput(stacks);
    }

    /** The recipe the assembler would run right now (ignoring the output slot), cached. */
    public RecipeHolder<AssemblyRecipe> bestRecipe() {
        if (level == null) {
            return null;
        }
        if (recipeDirty) {
            recipeDirty = false;
            AssemblyRecipeInput in = input();
            cachedRecipe = in.getItem(SLOT_PLATE).isEmpty() ? null : AssemblyRecipe.findBest(level, in, TIER_CIRCUITS.get(progress > 0 ? craftTier : tier).get());
            cachedSlots = cachedRecipe == null ? null : cachedRecipe.value().matchSlots(in);
        }
        return cachedRecipe;
    }

    /** Component slots (1..5) used by the current recipe, in recipe order, or empty. */
    public int[] usedSlots() {
        bestRecipe();
        return cachedSlots == null ? new int[0] : cachedSlots;
    }

    private RecipeHolder<AssemblyRecipe> runnableRecipe() {
        RecipeHolder<AssemblyRecipe> holder = bestRecipe();
        if (holder == null) {
            return null;
        }
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack result = holder.value().result();
        boolean fits = out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
        return fits ? holder : null;
    }

    @Override
    protected int energyPerTick() {
        return 40;
    }

    @Override
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, inventory.getStackInSlot(i));
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (working) {
                progress += WORK_PER_TICK;
            }
            return;
        }

        if (level.getGameTime() % 8 == 0) {
            ejectOutput(inventory, SLOT_OUTPUT);
        }

        if (progress <= 0 && craftTier != tier) {
            craftTier = tier;
            recipeDirty = true;
        }
        RecipeHolder<AssemblyRecipe> holder = isFormed() ? runnableRecipe() : null;
        boolean canWork = active && holder != null && hasEnergy();
        if (canWork != working) {
            working = canWork;
            sendData();
        }
        if (holder == null) {
            progress = 0;
        }
        if (!canWork) {
            return;
        }

        AssemblyRecipe recipe = holder.value();
        workNeeded = recipe.work();
        useEnergy();
        progress += WORK_PER_TICK;
        if (progress >= recipe.work()) {
            progress = 0;
            int[] slots = usedSlots().clone();
            ItemStack result = recipe.result().copy();
            inventory.extractItem(SLOT_PLATE, 1, false);
            for (int slot : slots) {
                inventory.extractItem(slot, 1, false);
            }
            // insertItem would refuse: the output slot is not a valid insert target (isItemValid), so set it directly.
            ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
            if (out.isEmpty()) {
                inventory.setStackInSlot(SLOT_OUTPUT, result);
            } else {
                ItemStack merged = out.copy();
                merged.grow(result.getCount());
                inventory.setStackInSlot(SLOT_OUTPUT, merged);
            }
            level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3f, 1.8f);
        }
    }

    // ---- interaction: any part of the formed factory opens the screen --------------------------

    @Override
    public ItemInteractionResult useItem(ItemStack stack, Player player, InteractionHand hand,
                                         BlockPos clicked, BlockHitResult hit) {
        return open(player) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult use(Player player, BlockPos clicked, BlockHitResult hit) {
        return open(player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    private boolean open(Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new CircuitAssemblerMenu(id, inv, this),
                            Component.translatable("block.createautocrafting.circuit_assembler_controller")),
                    buf -> buf.writeBlockPos(worldPosition));
        }
        return true;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Work", workNeeded);
        tag.putBoolean("Working", working);
        tag.putInt("Tier", tier);
        tag.putInt("CraftTier", craftTier);
        tag.putBoolean("Active", active);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        workNeeded = tag.contains("Work") ? tag.getInt("Work") : AssemblyRecipe.DEFAULT_WORK;
        working = tag.getBoolean("Working");
        tier = Math.max(0, Math.min(TIER_CIRCUITS.size() - 1, tag.getInt("Tier")));
        craftTier = tag.contains("CraftTier")
                ? Math.max(0, Math.min(TIER_CIRCUITS.size() - 1, tag.getInt("CraftTier"))) : tier;
        active = !tag.contains("Active") || tag.getBoolean("Active");
        recipeDirty = true;
    }
}
