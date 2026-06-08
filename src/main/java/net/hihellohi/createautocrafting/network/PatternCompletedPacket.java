package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PatternCompletedPacket {
    private final BlockPos pos;
    private final ItemStack resultItem;
    private final int amount;

    public PatternCompletedPacket(BlockPos pos, ItemStack resultItem, int amount) {
        this.pos = pos;
        this.resultItem = resultItem;
        this.amount = amount;
    }

    public PatternCompletedPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.resultItem = buf.readItem();
        this.amount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(resultItem);
        buf.writeInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.patternCompleted(pos, resultItem, amount)));
        context.setPacketHandled(true);
    }
}
