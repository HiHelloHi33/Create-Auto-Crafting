package net.hihellohi.createautocrafting.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration + send helpers. All payloads are {@link CustomPacketPayload} records with a
 * {@code STREAM_CODEC}; their handlers run via {@code context.enqueueWork(...)} on the right thread.
 */
public class ModPackets {
    private static final String PROTOCOL = "1";

    public static void register(IEventBus modBus) {
        modBus.addListener(ModPackets::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL);

        r.playToClient(PatternSyncPacket.TYPE, PatternSyncPacket.STREAM_CODEC, PatternSyncPacket::handle);
        r.playToServer(CraftRequestPacket.TYPE, CraftRequestPacket.STREAM_CODEC, CraftRequestPacket::handle);
        r.playToServer(CraftPlanRequestPacket.TYPE, CraftPlanRequestPacket.STREAM_CODEC, CraftPlanRequestPacket::handle);
        r.playToClient(CraftPlanPacket.TYPE, CraftPlanPacket.STREAM_CODEC, CraftPlanPacket::handle);
        r.playToClient(CraftDoneToastPacket.TYPE, CraftDoneToastPacket.STREAM_CODEC, CraftDoneToastPacket::handle);
        r.playToServer(PatternGhostPacket.TYPE, PatternGhostPacket.STREAM_CODEC, PatternGhostPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

}
