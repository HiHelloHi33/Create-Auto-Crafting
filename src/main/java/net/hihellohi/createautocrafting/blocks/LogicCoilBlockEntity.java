package net.hihellohi.createautocrafting.blocks;

import net.hihellohi.createautocrafting.manager.LogicCoilRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Logic Coil holds no data. It exists only to tell the {@link LogicCoilRegistry} when a coil
 * appears in (or leaves) a loaded chunk, so craft plans can count the coils near a network.
 */
public class LogicCoilBlockEntity extends BlockEntity {

    public LogicCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LOGIC_COIL_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            LogicCoilRegistry.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            LogicCoilRegistry.remove(level, worldPosition);
        }
    }
}
