package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: set ghost slot {@code slotIndex} (0..8 grid, 9 result) of the open Crafting
 * Blueprint menu to {@code stack}. Sent by the JEI ghost-ingredient drag handler, since a dragged
 * JEI item is not a real carried stack and so never reaches the menu's normal click path.
 */
public class BlueprintGhostPacket {
    private final int slotIndex;
    private final ItemStack stack;

    public BlueprintGhostPacket(int slotIndex, ItemStack stack) {
        this.slotIndex = slotIndex;
        this.stack = stack;
    }

    public BlueprintGhostPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.stack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeItem(stack);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof CraftingBlueprintMenu menu) {
                menu.setGhost(slotIndex, stack);
            }
        });
        context.setPacketHandled(true);
    }
}
