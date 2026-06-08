package net.hihellohi.createautocrafting.network;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.hihellohi.createautocrafting.manager.CraftJobManager;
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
 * Client -> server: craft {@code amount} of {@code result} via the ticker at {@code tickerPos}.
 * Creates a multi-step craft job; the {@link CraftJobManager} ships the recipe's raw ingredients to
 * the Crafting Distributor(s) — sub-crafts first, the final last — sourced from network storage and
 * routed at the network level (the ticker's address field is never changed).
 */
public class CraftRequestPacket {
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int amount;

    public CraftRequestPacket(BlockPos tickerPos, ItemStack result, int amount) {
        this.tickerPos = tickerPos;
        this.result = result;
        this.amount = amount;
    }

    public CraftRequestPacket(FriendlyByteBuf buf) {
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
            CraftJobManager.start(level, networkId, player.getUUID(), result, amount);
        });
        context.setPacketHandled(true);
    }
}
