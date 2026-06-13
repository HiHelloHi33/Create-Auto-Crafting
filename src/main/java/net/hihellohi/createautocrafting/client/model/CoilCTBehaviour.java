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
 * part of the <em>same</em> assembly (largest valid prism — see {@link LogicCoilRegistry#assemblyOf}).
 * So a loose coil stuck to a finished prism doesn't tear the prism's texture: the prism keeps its
 * border on that face and the loose coil is excluded.
 *
 * <p>{@code connectsTo} is queried for every face of the same position in a row, so we keep a tiny
 * per-thread cache of the last position's assembly to avoid recomputing it eight times.
 */
public class CoilCTBehaviour extends HorizontalCTBehaviour {

    private final ThreadLocal<BlockPos> lastPos = new ThreadLocal<>();
    private final ThreadLocal<int[]> lastAssembly = new ThreadLocal<>();

    public CoilCTBehaviour(CTSpriteShiftEntry side, CTSpriteShiftEntry top) {
        super(side, top);
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader,
                              BlockPos pos, BlockPos otherPos, Direction face) {
        if (!super.connectsTo(state, other, reader, pos, otherPos, face)) {
            return false;
        }
        return LogicCoilRegistry.inAssembly(assemblyFor(reader, pos), otherPos);
    }

    private int[] assemblyFor(BlockAndTintGetter reader, BlockPos pos) {
        if (pos.equals(lastPos.get())) {
            return lastAssembly.get();
        }
        int[] assembly = LogicCoilRegistry.assemblyOf(reader, pos);
        lastPos.set(pos.immutable());
        lastAssembly.set(assembly);
        return assembly;
    }
}
