package net.hihellohi.createautocrafting.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Takes Forge Energy from cables and feeds the factory it belongs to. Right-click to read its charge. */
public class EnergyInputBlock extends FactoryMemberBlock implements EntityBlock {
    public EnergyInputBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(2.5f)
                .sound(SoundType.COPPER)
                .noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyInputBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlocks.ENERGY_INPUT_BE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((EnergyInputBlockEntity) be).serverTick();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionResult forwarded = super.useWithoutItem(state, level, pos, player, hit);
        if (forwarded != InteractionResult.PASS) {
            return forwarded;
        }
        if (level.getBlockEntity(pos) instanceof EnergyInputBlockEntity be) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.createautocrafting.energy",
                        be.storage.getEnergyStored(), be.storage.getMaxEnergyStored()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
