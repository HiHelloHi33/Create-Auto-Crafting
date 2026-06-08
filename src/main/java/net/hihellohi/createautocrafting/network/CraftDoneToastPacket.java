package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: a requested craft has returned to the network; show a finished toast. */
public class CraftDoneToastPacket {
    private final ItemStack result;
    private final int amount;

    public CraftDoneToastPacket(ItemStack result, int amount) {
        this.result = result;
        this.amount = amount;
    }

    public CraftDoneToastPacket(FriendlyByteBuf buf) {
        this.result = buf.readItem();
        this.amount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(result);
        buf.writeVarInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.showCraftDone(result, amount)));
        context.setPacketHandled(true);
    }
}
