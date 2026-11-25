package net.hihellohi.createautocrafting.blocks;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A factory controller: right-click it to (re)check the 2x2 around it; once formed it runs the factory. */
public abstract class FactoryControllerBlock<T extends FactoryControllerBlockEntity> extends FactoryMemberBlock
        implements IBE<T> {

    protected FactoryControllerBlock(Properties properties) {
        super(properties);
    }

    public abstract FactoryKind kind();

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FactoryControllerBlockEntity be) {
            be.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(FORMED)) {
            if (!level.isClientSide) {
                boolean formed = Formation.tryForm(level, pos);
                player.displayClientMessage(Component.translatable(formed
                        ? "message.createautocrafting.formed" : "message.createautocrafting.not_formed"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return IBE.super.newBlockEntity(pos, state);
    }
}
