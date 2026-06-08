package net.hihellohi.createautocrafting.items;

import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

public class PackagerCrafterKitItem extends Item {
    public PackagerCrafterKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!(state.getBlock() instanceof PackagerBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PackagerBlockEntity packager)) {
            return InteractionResult.PASS;
        }

        if (PatternProviderHelper.isPatternProvider(packager)) {
            return InteractionResult.FAIL;
        }

        if (!canPlayerModifyBlock(serverPlayer, pos)) {
            return InteractionResult.FAIL;
        }

        convertToPatternProvider((ServerLevel) level, pos, packager, serverPlayer);

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    private boolean canPlayerModifyBlock(ServerPlayer player, BlockPos pos) {
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(
                player.serverLevel(), pos, player.serverLevel().getBlockState(pos), player));
    }

    private void convertToPatternProvider(ServerLevel level, BlockPos pos,
                                          PackagerBlockEntity packager, ServerPlayer player) {
        PatternProviderHelper.markPatternProvider(packager, player.getUUID(), player.getName().getString());

        level.playSound(null, pos, SoundEvents.ARROW_HIT_PLAYER, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5f, 1.5f);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.1);
    }
}
