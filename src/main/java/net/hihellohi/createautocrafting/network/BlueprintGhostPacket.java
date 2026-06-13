package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: set ghost slot {@code slotIndex} (0..8 grid, 9 result) of the open Crafting
 * Blueprint menu to {@code stack}. Sent by the JEI ghost-ingredient drag handler.
 */
public record BlueprintGhostPacket(int slotIndex, ItemStack stack) implements CustomPacketPayload {

    public static final Type<BlueprintGhostPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "blueprint_ghost"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintGhostPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BlueprintGhostPacket::slotIndex,
                    ItemStack.OPTIONAL_STREAM_CODEC, BlueprintGhostPacket::stack,
                    BlueprintGhostPacket::new);

    @Override
    public Type<BlueprintGhostPacket> type() {
        return TYPE;
    }

    public static void handle(BlueprintGhostPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof CraftingBlueprintMenu menu) {
                menu.setGhost(packet.slotIndex, packet.stack);
            }
        });
    }
}
