package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * The brain of a 2x2 factory, living in the controller block: it holds the inventory, runs the recipes
 * and draws Forge Energy from the factory's Energy Input. It only works while the multiblock is formed.
 */
public abstract class FactoryControllerBlockEntity extends SmartBlockEntity {
    /** Work done per tick while running (recipes list how much work they need). */
    public static final int WORK_PER_TICK = 2;

    protected int progress;
    protected int workNeeded;
    /** Synced so clients can animate the machine. */
    protected boolean working;

    protected FactoryControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    /** Forge Energy used every tick while crafting. */
    protected abstract int energyPerTick();

    /** Spill the inventory (the block was broken). */
    public abstract void dropContents();

    public boolean isFormed() {
        return getBlockState().getValue(FactoryMemberBlock.FORMED);
    }

    public boolean isWorking() {
        return working;
    }

    /** Whether the Energy Input has enough charge for one more tick of work. */
    protected boolean hasEnergy() {
        EnergyInputBlockEntity input = level == null ? null
                : Formation.findEnergyInput(level, worldPosition, getBlockState());
        return input != null && input.draw(energyPerTick(), true) >= energyPerTick();
    }

    protected void useEnergy() {
        EnergyInputBlockEntity input = Formation.findEnergyInput(level, worldPosition, getBlockState());
        if (input != null) {
            input.draw(energyPerTick(), false);
        }
    }

    /** Progress of the current craft as a 0..1 fraction, smoothed between ticks. */
    public float getProgress(float partialTicks) {
        float p = progress + (working ? WORK_PER_TICK * partialTicks : 0);
        return Math.min(1f, p / Math.max(1, workNeeded));
    }

    public void onFormed() {
        setChanged();
        sendData();
    }

    public void onUnformed() {
        dropContents(); // never leave items trapped in a machine that isn't formed
        working = false;
        progress = 0;
        setChanged();
        sendData();
    }

    /** A click with an item on any part of the formed factory. */
    public abstract ItemInteractionResult useItem(ItemStack stack, Player player, InteractionHand hand,
                                                  BlockPos clicked, BlockHitResult hit);

    /** A click with an empty hand on any part of the formed factory. */
    public abstract InteractionResult use(Player player, BlockPos clicked, BlockHitResult hit);

    /**
     * Pushes the finished product out into any inventory touching the 2x2 (chests, hoppers, depots,
     * even another factory), so it doesn't sit inside the machine. Call a few times a second.
     */
    protected void ejectOutput(ItemStackHandler inv, int slot) {
        if (level == null || level.isClientSide || !isFormed() || inv.getStackInSlot(slot).isEmpty()) {
            return;
        }
        Direction facing = getBlockState().getValue(FactoryMemberBlock.FACING);
        List<BlockPos> parts = new java.util.ArrayList<>();
        for (FactoryMemberBlock.Part part : FactoryMemberBlock.Part.values()) {
            parts.add(Formation.partPos(worldPosition, facing, part));
        }
        for (BlockPos part : parts) {
            for (Direction dir : Direction.values()) {
                BlockPos target = part.relative(dir);
                if (parts.contains(target)) {
                    continue;
                }
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, dir.getOpposite());
                if (handler == null) {
                    continue;
                }
                ItemStack out = inv.getStackInSlot(slot);
                if (out.isEmpty()) {
                    return;
                }
                ItemStack remainder = ItemHandlerHelper.insertItem(handler, out.copy(), false);
                int moved = out.getCount() - remainder.getCount();
                if (moved > 0) {
                    inv.extractItem(slot, moved, false);
                }
            }
        }
    }
}
