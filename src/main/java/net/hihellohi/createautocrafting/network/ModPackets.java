package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPackets {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateAutoCrafting.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(VirtualStockSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VirtualStockSyncPacket::encode)
                .decoder(VirtualStockSyncPacket::new)
                .consumerMainThread(VirtualStockSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(PatternRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PatternRequestPacket::encode)
                .decoder(PatternRequestPacket::new)
                .consumerMainThread(PatternRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(PatternCompletedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PatternCompletedPacket::encode)
                .decoder(PatternCompletedPacket::new)
                .consumerMainThread(PatternCompletedPacket::handle)
                .add();

        CHANNEL.messageBuilder(PatternSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PatternSyncPacket::encode)
                .decoder(PatternSyncPacket::new)
                .consumerMainThread(PatternSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CraftRequestPacket::encode)
                .decoder(CraftRequestPacket::new)
                .consumerMainThread(CraftRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftPlanRequestPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CraftPlanRequestPacket::encode)
                .decoder(CraftPlanRequestPacket::new)
                .consumerMainThread(CraftPlanRequestPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftPlanPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CraftPlanPacket::encode)
                .decoder(CraftPlanPacket::new)
                .consumerMainThread(CraftPlanPacket::handle)
                .add();

        CHANNEL.messageBuilder(CraftDoneToastPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CraftDoneToastPacket::encode)
                .decoder(CraftDoneToastPacket::new)
                .consumerMainThread(CraftDoneToastPacket::handle)
                .add();

        CHANNEL.messageBuilder(BlueprintGhostPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BlueprintGhostPacket::encode)
                .decoder(BlueprintGhostPacket::new)
                .consumerMainThread(BlueprintGhostPacket::handle)
                .add();
    }

    public static <MSG> void sendToChunk(MSG packet, LevelChunk chunk) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
    }

    public static <MSG> void sendToPlayer(MSG packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <MSG> void sendToServer(MSG packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }
}
