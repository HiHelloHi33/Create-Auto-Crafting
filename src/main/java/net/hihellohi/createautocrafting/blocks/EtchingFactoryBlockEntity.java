package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.etching.EtchingRecipe;
import net.hihellohi.createautocrafting.etching.EtchingRecipeInput;
import net.hihellohi.createautocrafting.etching.ModRecipes;
import net.hihellohi.createautocrafting.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * The Etching Factory's brain (lives in the controller corner). A tank of etching acid, one plate being
 * etched and one finished plate. While rotation is applied it dips the plate in the acid, using the
 * recipe's fluid amount (100 mB for a tier 1 plate, 250 mB for tier 2).
 */
public class EtchingFactoryBlockEntity extends FactoryControllerBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int TANK_CAPACITY = 4000;
    /** 100 mB per second while the multiblock is broken. */
    public static final int ACID_LOSS_PER_TICK = 5;

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            recipeDirty = true;
            changed();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && isBaseItem(stack);
        }
    };

    public final FluidTank tank = new FluidTank(TANK_CAPACITY,
            fluid -> fluid.getFluid().isSame(ModFluids.ETCHING_ACID.get())) {
        @Override
        protected void onContentsChanged() {
            changed();
        }
    };

    /** What hoppers, funnels and belts see: insert plates, extract only finished ones. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
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
            return slot == SLOT_INPUT && inventory.isItemValid(slot, stack);
        }
    };

    private boolean recipeDirty = true;
    private RecipeHolder<EtchingRecipe> cachedRecipe;

    public EtchingFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ETCHING_FACTORY_BE.get(), pos, state);
        workNeeded = EtchingRecipe.DEFAULT_WORK;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData(); // the basin is drawn from this, so clients need every change
        }
    }

    public IItemHandler getAutomationHandler() {
        return automation;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    // ---- recipe lookup ------------------------------------------------------------------------

    private boolean isBaseItem(ItemStack stack) {
        return level == null || level.getRecipeManager().getAllRecipesFor(ModRecipes.ETCHING_TYPE.get()).stream()
                .anyMatch(h -> h.value().base().test(stack));
    }

    public boolean canAccept(ItemStack stack) {
        return inventory.insertItem(SLOT_INPUT, stack, true).getCount() < stack.getCount();
    }

    /** The recipe for the loaded plate, if the acid, the output slot and the plate allow it. */
    public RecipeHolder<EtchingRecipe> currentRecipe() {
        if (level == null) {
            return null;
        }
        if (recipeDirty) {
            recipeDirty = false;
            ItemStack base = inventory.getStackInSlot(SLOT_INPUT);
            Optional<RecipeHolder<EtchingRecipe>> found = base.isEmpty() ? Optional.empty()
                    : level.getRecipeManager().getRecipeFor(ModRecipes.ETCHING_TYPE.get(),
                    new EtchingRecipeInput(base), level);
            cachedRecipe = found.orElse(null);
        }
        if (cachedRecipe == null) {
            return null;
        }
        EtchingRecipe recipe = cachedRecipe.value();
        if (!recipe.fluid().test(tank.getFluid())) {
            return null;
        }
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack result = recipe.result();
        boolean fits = out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
        return fits ? cachedRecipe : null;
    }

    // ---- reading for the renderer -------------------------------------------------------------

    public ItemStack getInput() {
        return inventory.getStackInSlot(SLOT_INPUT);
    }

    public ItemStack getOutput() {
        return inventory.getStackInSlot(SLOT_OUTPUT);
    }

    public FluidStack getFluid() {
        return tank.getFluid();
    }

    /** The result of what's being etched (for showing the plate change while it's under the acid). */
    public ItemStack previewResult() {
        RecipeHolder<EtchingRecipe> holder = cachedRecipe;
        return holder == null ? ItemStack.EMPTY : holder.value().result();
    }

    public boolean hasAnyItems() {
        return !getInput().isEmpty() || !getOutput().isEmpty();
    }

    // ---- player interaction -------------------------------------------------------------------

    public boolean insertFromPlayer(Player player, InteractionHand hand, ItemStack held) {
        ItemStack remainder = inventory.insertItem(SLOT_INPUT, held.copy(), false);
        if (remainder.getCount() == held.getCount()) {
            return false;
        }
        if (!player.isCreative()) {
            player.setItemInHand(hand, remainder);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1.2f);
        return true;
    }

    /** Right-click with an empty hand: take the finished plates, or else the loaded ones back. */
    public boolean takeToPlayer(Player player) {
        for (int slot : new int[]{SLOT_OUTPUT, SLOT_INPUT}) {
            ItemStack taken = inventory.extractItem(slot, inventory.getSlotLimit(slot), false);
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1.0f);
                return true;
            }
        }
        return false;
    }

    @Override
    protected int energyPerTick() {
        return 20;
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

    // ---- ticking ------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            if (working) {
                progress += WORK_PER_TICK; // predicted; corrected whenever the server sends data
            }
            currentRecipe(); // keeps the plate preview fresh for the renderer
            return;
        }

        if (level.getGameTime() % 8 == 0) {
            ejectOutput(inventory, SLOT_OUTPUT);
        }

        // Without a formed basin the acid has nowhere to sit: it drains away at 100 mB/s.
        if (!isFormed() && !tank.isEmpty()) {
            tank.drain(ACID_LOSS_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
            return;
        }

        RecipeHolder<EtchingRecipe> holder = isFormed() ? currentRecipe() : null;
        boolean canWork = holder != null && hasEnergy();
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

        EtchingRecipe recipe = holder.value();
        workNeeded = recipe.work();
        useEnergy();
        progress += WORK_PER_TICK;
        if (progress >= recipe.work()) {
            progress = 0;
            tank.drain(recipe.fluid().amount(), IFluidHandler.FluidAction.EXECUTE);
            inventory.extractItem(SLOT_INPUT, 1, false);
            inventory.insertItem(SLOT_OUTPUT, recipe.result().copy(), false);
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5f, 1.4f);
        }
    }

    // ---- interaction: no GUI, everything happens in the basin ----------------------------------

    @Override
    public ItemInteractionResult useItem(ItemStack stack, Player player, InteractionHand hand,
                                         BlockPos clicked, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Buckets and other fluid containers fill or drain the basin.
        if (FluidUtil.interactWithFluidHandler(player, hand, level, clicked, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return canAccept(stack) ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return insertFromPlayer(player, hand, stack) ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public InteractionResult use(Player player, BlockPos clicked, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return hasAnyItems() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return takeToPlayer(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("Work", workNeeded);
        tag.putBoolean("Working", working);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        progress = tag.getInt("Progress");
        workNeeded = tag.contains("Work") ? tag.getInt("Work") : EtchingRecipe.DEFAULT_WORK;
        working = tag.getBoolean("Working");
        recipeDirty = true;
    }
}
