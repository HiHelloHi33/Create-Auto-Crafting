package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: a requested craft has returned to the network; show a finished toast. */
public record CraftDoneToastPacket(ItemStack result, int amount) implements CustomPacketPayload {

    public static final Type<CraftDoneToastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "craft_done_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftDoneToastPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC, CraftDoneToastPacket::result,
                    ByteBufCodecs.VAR_INT, CraftDoneToastPacket::amount,
                    CraftDoneToastPacket::new);

    @Override
    public Type<CraftDoneToastPacket> type() {
        return TYPE;
    }

    public static void handle(CraftDoneToastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.showCraftDone(packet.result, packet.amount));
    }
}
