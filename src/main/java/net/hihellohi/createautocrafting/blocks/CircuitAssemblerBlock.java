package net.hihellohi.createautocrafting.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Controller of the Circuit Assembler: a 2x2 glass-walled pick-and-place machine with a screen. */
public class CircuitAssemblerBlock extends FactoryControllerBlock<CircuitAssemblerBlockEntity> {

    public CircuitAssemblerBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.5f)
                .sound(SoundType.COPPER)
                .noOcclusion());
    }

    @Override
    public FactoryKind kind() {
        return FactoryKind.ASSEMBLER;
    }

    @Override
    public Class<CircuitAssemblerBlockEntity> getBlockEntityClass() {
        return CircuitAssemblerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CircuitAssemblerBlockEntity> getBlockEntityType() {
        return ModBlocks.CIRCUIT_ASSEMBLER_BE.get();
    }

}
