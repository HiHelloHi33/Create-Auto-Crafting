package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.menu.PatternCardMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: set ghost slot {@code slotIndex} (0-8 grid, 9 result) of the open Pattern Card
 * menu to {@code stack}. Sent by the JEI drag-and-drop handler.
 */
public record PatternGhostPacket(int slotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<PatternGhostPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "pattern_ghost"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternGhostPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PatternGhostPacket::slotIndex,
                    ItemStack.OPTIONAL_STREAM_CODEC, PatternGhostPacket::stack,
                    PatternGhostPacket::new);

    @Override
    public Type<PatternGhostPacket> type() {
        return TYPE;
    }

    public static void handle(PatternGhostPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof PatternCardMenu menu) {
                menu.setGhost(packet.slotIndex, packet.stack);
            }
        });
    }
}
