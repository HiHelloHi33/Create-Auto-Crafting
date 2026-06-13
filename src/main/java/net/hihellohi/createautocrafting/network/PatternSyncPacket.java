package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/**
 * Server -> client: the craftable patterns available on a logistics network. Cached client-side in
 * {@code ClientPatternCache} so the request screen can show them as craftable on dedicated servers.
 */
public record PatternSyncPacket(UUID networkId, List<CraftingPattern> patterns)
        implements CustomPacketPayload {

    public static final Type<PatternSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "pattern_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PatternSyncPacket::networkId,
                    CraftingPattern.STREAM_CODEC.apply(ByteBufCodecs.list()), PatternSyncPacket::patterns,
                    PatternSyncPacket::new);

    @Override
    public Type<PatternSyncPacket> type() {
        return TYPE;
    }

    public static void handle(PatternSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.onPatternsSynced(packet.networkId, packet.patterns));
    }
}
