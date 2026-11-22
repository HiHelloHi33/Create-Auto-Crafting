package net.hihellohi.createautocrafting.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Logic Coil. It has no orientation or assembled blockstate — its look is driven entirely by
 * Create's connected textures (see {@code CoilCTModel}), so adjacent coils tile seamlessly like a
 * casing. Multiblock validity is computed separately in {@code LogicCoilRegistry}.
 */
public class LogicCoilBlock extends BaseEntityBlock {

    public static final MapCodec<LogicCoilBlock> CODEC = simpleCodec(p -> new LogicCoilBlock());

    public LogicCoilBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.5f)
                .sound(SoundType.COPPER)
                .noOcclusion());
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogicCoilBlockEntity(pos, state);
    }
}
