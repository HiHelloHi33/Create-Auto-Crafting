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

/** Controller of the Etching Factory: a 2x2 basin of etching acid. All the work happens in the basin. */
public class EtchingFactoryBlock extends FactoryControllerBlock<EtchingFactoryBlockEntity> {

    public EtchingFactoryBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(2.5f)
                .sound(SoundType.COPPER)
                .noOcclusion());
    }

    @Override
    public FactoryKind kind() {
        return FactoryKind.ETCHER;
    }

    @Override
    public Class<EtchingFactoryBlockEntity> getBlockEntityClass() {
        return EtchingFactoryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EtchingFactoryBlockEntity> getBlockEntityType() {
        return ModBlocks.ETCHING_FACTORY_BE.get();
    }

}
