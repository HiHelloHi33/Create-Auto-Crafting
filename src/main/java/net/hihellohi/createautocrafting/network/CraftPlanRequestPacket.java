package net.hihellohi.createautocrafting.network;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.manager.CraftingPlanner;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
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
 * Client -> server: compute a {@link CraftingPlan} for the requested craft and send it back so the
 * preview screen can be shown before the player commits.
 */
public record CraftPlanRequestPacket(BlockPos tickerPos, ItemStack result, int amount)
        implements CustomPacketPayload {

    public static final Type<CraftPlanRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "craft_plan_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPlanRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CraftPlanRequestPacket::tickerPos,
                    ItemStack.OPTIONAL_STREAM_CODEC, CraftPlanRequestPacket::result,
                    ByteBufCodecs.VAR_INT, CraftPlanRequestPacket::amount,
                    CraftPlanRequestPacket::new);

    @Override
    public Type<CraftPlanRequestPacket> type() {
        return TYPE;
    }

    public static void handle(CraftPlanRequestPacket packet, IPayloadContext context) {
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
            CraftingPlan plan = CraftingPlanner.plan(level, networkId, packet.result, packet.amount);
            ModPackets.sendToPlayer(new CraftPlanPacket(packet.tickerPos, packet.result, packet.amount, plan), player);
        });
    }
}
