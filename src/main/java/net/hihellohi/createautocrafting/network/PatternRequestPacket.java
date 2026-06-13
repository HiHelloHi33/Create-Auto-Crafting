package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.blocks.LogicCoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternRequestPacket(BlockPos coilPos, ItemStack requestedItem, int amount)
        implements CustomPacketPayload {

    public static final Type<PatternRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "pattern_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PatternRequestPacket::coilPos,
                    ItemStack.OPTIONAL_STREAM_CODEC, PatternRequestPacket::requestedItem,
                    ByteBufCodecs.VAR_INT, PatternRequestPacket::amount,
                    PatternRequestPacket::new);

    @Override
    public Type<PatternRequestPacket> type() {
        return TYPE;
    }

    public static void handle(PatternRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (level.isLoaded(packet.coilPos) && player.distanceToSqr(
                    packet.coilPos.getX() + 0.5, packet.coilPos.getY() + 0.5, packet.coilPos.getZ() + 0.5) < 64) {
                BlockEntity be = level.getBlockEntity(packet.coilPos);
                if (be instanceof LogicCoilBlockEntity coil && coil.canModify(player)) {
                    coil.requestCrafting(packet.requestedItem, packet.amount, player);
                }
            }
        });
    }
}
