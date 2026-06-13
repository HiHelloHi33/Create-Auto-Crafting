package net.hihellohi.createautocrafting.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge payload registration + send helpers. Replaces the 1.20.1 Forge {@code SimpleChannel}.
 * All payloads are {@link CustomPacketPayload} records with a {@code STREAM_CODEC}; their handlers
 * run via {@code context.enqueueWork(...)} on the appropriate side.
 */
public class ModPackets {
    private static final String PROTOCOL = "1";

    public static void register(IEventBus modBus) {
        modBus.addListener(ModPackets::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);

        r.playToClient(VirtualStockSyncPacket.TYPE, VirtualStockSyncPacket.STREAM_CODEC, VirtualStockSyncPacket::handle);
        r.playToServer(PatternRequestPacket.TYPE, PatternRequestPacket.STREAM_CODEC, PatternRequestPacket::handle);
        r.playToClient(PatternCompletedPacket.TYPE, PatternCompletedPacket.STREAM_CODEC, PatternCompletedPacket::handle);
        r.playToClient(PatternSyncPacket.TYPE, PatternSyncPacket.STREAM_CODEC, PatternSyncPacket::handle);
        r.playToServer(CraftRequestPacket.TYPE, CraftRequestPacket.STREAM_CODEC, CraftRequestPacket::handle);
        r.playToServer(CraftPlanRequestPacket.TYPE, CraftPlanRequestPacket.STREAM_CODEC, CraftPlanRequestPacket::handle);
        r.playToClient(CraftPlanPacket.TYPE, CraftPlanPacket.STREAM_CODEC, CraftPlanPacket::handle);
        r.playToClient(CraftDoneToastPacket.TYPE, CraftDoneToastPacket.STREAM_CODEC, CraftDoneToastPacket::handle);
        r.playToServer(BlueprintGhostPacket.TYPE, BlueprintGhostPacket.STREAM_CODEC, BlueprintGhostPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToChunk(CustomPacketPayload payload, LevelChunk chunk) {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) chunk.getLevel(), chunk.getPos(), payload);
    }
}
