package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PatternCompletedPacket(BlockPos pos, ItemStack resultItem, int amount)
        implements CustomPacketPayload {

    public static final Type<PatternCompletedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "pattern_completed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternCompletedPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PatternCompletedPacket::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC, PatternCompletedPacket::resultItem,
                    ByteBufCodecs.VAR_INT, PatternCompletedPacket::amount,
                    PatternCompletedPacket::new);

    @Override
    public Type<PatternCompletedPacket> type() {
        return TYPE;
    }

    public static void handle(PatternCompletedPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.patternCompleted(packet.pos, packet.resultItem, packet.amount));
    }
}
