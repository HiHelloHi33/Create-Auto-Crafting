package net.hihellohi.createautocrafting.blocks;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The body block of a factory. Alone it has a connected texture; in a formed factory it is part of the shell. */
public class FactoryCoreBlock extends FactoryMemberBlock {
    public FactoryCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(2.5f)
                .sound(SoundType.COPPER)
                .noOcclusion());
    }
}
