package net.hihellohi.createautocrafting.network;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.manager.CraftJobManager;
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

import java.util.UUID;

/**
 * Client -> server: craft {@code amount} of {@code result} via the ticker at {@code tickerPos}.
 * Creates a multi-step craft job; the {@link CraftJobManager} ships the recipe's raw ingredients to
 * the Crafting Distributor(s), sourced from network storage and routed at the network level.
 */
public record CraftRequestPacket(BlockPos tickerPos, ItemStack result, int amount)
        implements CustomPacketPayload {

    public static final Type<CraftRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CraftRequestPacket::tickerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC, CraftRequestPacket::result,
                    ByteBufCodecs.VAR_INT, CraftRequestPacket::amount,
                    CraftRequestPacket::new);

    @Override
    public Type<CraftRequestPacket> type() {
        return TYPE;
    }

    public static void handle(CraftRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || packet.amount <= 0) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!level.isLoaded(packet.tickerPos)
                    || player.distanceToSqr(packet.tickerPos.getX() + 0.5, packet.tickerPos.getY() + 0.5,
                    packet.tickerPos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            BlockEntity be = level.getBlockEntity(packet.tickerPos);
            if (!(be instanceof StockTickerBlockEntity ticker) || ticker.behaviour == null) {
                return;
            }
            UUID networkId = ticker.behaviour.freqId;
            if (networkId == null) {
                return;
            }
            CraftJobManager.start(level, networkId, player.getUUID(), packet.result, packet.amount);
        });
    }
}
