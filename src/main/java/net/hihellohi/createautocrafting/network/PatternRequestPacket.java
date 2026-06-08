package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.blocks.LogicCoilBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PatternRequestPacket {
    private final BlockPos coilPos;
    private final ItemStack requestedItem;
    private final int amount;

    public PatternRequestPacket(BlockPos coilPos, ItemStack item, int amount) {
        this.coilPos = coilPos;
        this.requestedItem = item;
        this.amount = amount;
    }

    public PatternRequestPacket(FriendlyByteBuf buf) {
        this.coilPos = buf.readBlockPos();
        this.requestedItem = buf.readItem();
        this.amount = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(coilPos);
        buf.writeItem(requestedItem);
        buf.writeInt(amount);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();

                if (level.isLoaded(coilPos) && player.distanceToSqr(
                        coilPos.getX() + 0.5, coilPos.getY() + 0.5, coilPos.getZ() + 0.5) < 64) {

                    BlockEntity be = level.getBlockEntity(coilPos);
                    if (be instanceof LogicCoilBlockEntity coil) {
                        if (coil.canModify(player)) {
                            coil.requestCrafting(requestedItem, amount, player);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}