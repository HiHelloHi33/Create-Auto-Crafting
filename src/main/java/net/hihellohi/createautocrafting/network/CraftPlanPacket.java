package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: the computed plan for a requested craft. Opens the crafting-tree preview screen.
 */
public class CraftPlanPacket {
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int amount;
    private final CraftingPlan plan;

    public CraftPlanPacket(BlockPos tickerPos, ItemStack result, int amount, CraftingPlan plan) {
        this.tickerPos = tickerPos;
        this.result = result;
        this.amount = amount;
        this.plan = plan;
    }

    public CraftPlanPacket(FriendlyByteBuf buf) {
        this.tickerPos = buf.readBlockPos();
        this.result = buf.readItem();
        this.amount = buf.readVarInt();
        this.plan = CraftingPlan.read(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(tickerPos);
        buf.writeItem(result);
        buf.writeVarInt(amount);
        plan.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.openCraftPlan(tickerPos, result, amount, plan)));
        context.setPacketHandled(true);
    }
}
