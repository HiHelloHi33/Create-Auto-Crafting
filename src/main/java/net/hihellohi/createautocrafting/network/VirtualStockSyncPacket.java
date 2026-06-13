package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.integration.VirtualStockLink;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VirtualStockSyncPacket(BlockPos pos, ItemStack displayItem, int currentAmount, int targetAmount)
        implements CustomPacketPayload {

    public static final Type<VirtualStockSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "virtual_stock_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VirtualStockSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, VirtualStockSyncPacket::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC, VirtualStockSyncPacket::displayItem,
                    ByteBufCodecs.VAR_INT, VirtualStockSyncPacket::currentAmount,
                    ByteBufCodecs.VAR_INT, VirtualStockSyncPacket::targetAmount,
                    VirtualStockSyncPacket::new);

    public VirtualStockSyncPacket(BlockPos pos, VirtualStockLink stock) {
        this(pos, stock.getItem(), stock.getCurrentAmount(), stock.getTargetAmount());
    }

    @Override
    public Type<VirtualStockSyncPacket> type() {
        return TYPE;
    }

    public static void handle(VirtualStockSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandlers.virtualStockSync(
                packet.pos, packet.displayItem, packet.currentAmount, packet.targetAmount));
    }
}
