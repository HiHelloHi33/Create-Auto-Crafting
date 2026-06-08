package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.integration.VirtualStockLink;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VirtualStockSyncPacket {
    private final BlockPos pos;
    private final ItemStack displayItem;
    private final int currentAmount;
    private final int targetAmount;

    public VirtualStockSyncPacket(BlockPos pos, VirtualStockLink stock) {
        this.pos = pos;
        this.displayItem = stock.getItem();
        this.currentAmount = stock.getCurrentAmount();
        this.targetAmount = stock.getTargetAmount();
    }

    public VirtualStockSyncPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.displayItem = buf.readItem();
        this.currentAmount = buf.readInt();
        this.targetAmount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(displayItem);
        buf.writeInt(currentAmount);
        buf.writeInt(targetAmount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Client-only logic is reached via DistExecutor so the dedicated server never classloads it.
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.virtualStockSync(pos, displayItem, currentAmount, targetAmount)));
        context.setPacketHandled(true);
    }
}
