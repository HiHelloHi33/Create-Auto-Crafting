package net.hihellohi.createautocrafting.network;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.hihellohi.createautocrafting.manager.CraftingPlanner;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> server: compute a {@link CraftingPlan} for the requested craft and send it back so the
 * preview screen can be shown before the player commits.
 */
public class CraftPlanRequestPacket {
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int amount;

    public CraftPlanRequestPacket(BlockPos tickerPos, ItemStack result, int amount) {
        this.tickerPos = tickerPos;
        this.result = result;
        this.amount = amount;
    }

    public CraftPlanRequestPacket(FriendlyByteBuf buf) {
        this.tickerPos = buf.readBlockPos();
        this.result = buf.readItem();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(tickerPos);
        buf.writeItem(result);
        buf.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || amount <= 0) {
                return;
            }
            ServerLevel level = player.serverLevel();
            if (!level.isLoaded(tickerPos)
                    || player.distanceToSqr(tickerPos.getX() + 0.5, tickerPos.getY() + 0.5, tickerPos.getZ() + 0.5) > 64 * 64) {
                return;
            }
            BlockEntity be = level.getBlockEntity(tickerPos);
            if (!(be instanceof StockTickerBlockEntity ticker) || ticker.behaviour == null) {
                return;
            }
            UUID networkId = ticker.behaviour.freqId;
            if (networkId == null) {
                return;
            }
            CraftingPlan plan = CraftingPlanner.plan(level, networkId, result, amount);
            ModPackets.sendToPlayer(new CraftPlanPacket(tickerPos, result, amount, plan), player);
        });
        context.setPacketHandled(true);
    }
}
