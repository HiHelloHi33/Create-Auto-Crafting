package net.hihellohi.createautocrafting.client.model;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.hihellohi.createautocrafting.manager.LogicCoilRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's horizontal connected-texture behaviour, but coils only connect to neighbours that are
 * part of the <em>same</em> assembly (the largest valid prism — see {@link LogicCoilRegistry#assemblyOf}).
 * So a loose coil stuck to a finished prism doesn't tear the prism's texture.
 *
 * <p>{@code connectsTo} is asked about every face of one block in a row, so {@link CoilCTModel} works
 * out the assembly once and hands it over through {@link #CURRENT_ASSEMBLY} for the duration of that
 * block's model-data build. Nothing is cached between blocks, so it can never go stale.
 */
public class CoilCTBehaviour extends HorizontalCTBehaviour {

    /** The assembly {@code {minX,minY,minZ,dx,dy,dz}} of the block whose model data is being built. */
    static final ThreadLocal<int[]> CURRENT_ASSEMBLY = new ThreadLocal<>();

    public CoilCTBehaviour(CTSpriteShiftEntry side, CTSpriteShiftEntry top) {
        super(side, top);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader,
                              BlockPos pos, BlockPos otherPos, Direction face) {
        return super.connectsTo(state, other, reader, pos, otherPos, face)
                && LogicCoilRegistry.inAssembly(CURRENT_ASSEMBLY.get(), otherPos);
    }
}
