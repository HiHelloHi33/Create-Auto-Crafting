package net.hihellohi.createautocrafting.network;

import net.hihellohi.createautocrafting.client.ClientPacketHandlers;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> client: the craftable patterns available on a logistics network. Sent when the player
 * opens/queries a Stock Ticker; cached client-side in {@link ClientPatternCache} so the request
 * screen can show them as craftable on dedicated servers. Carries only common types, so its handler
 * does not need DistExecutor (no client classes are referenced).
 */
public class PatternSyncPacket {
    private final UUID networkId;
    private final List<CraftingPattern> patterns;

    public PatternSyncPacket(UUID networkId, List<CraftingPattern> patterns) {
        this.networkId = networkId;
        this.patterns = patterns;
    }

    public PatternSyncPacket(FriendlyByteBuf buf) {
        this.networkId = buf.readUUID();
        int count = buf.readVarInt();
        this.patterns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack result = buf.readItem();
            int outputCount = buf.readVarInt();
            int ingredientCount = buf.readVarInt();
            List<ItemStack> ingredients = new ArrayList<>(ingredientCount);
            for (int j = 0; j < ingredientCount; j++) {
                ingredients.add(buf.readItem());
            }
            patterns.add(new CraftingPattern(result, ingredients, outputCount));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(networkId);
        buf.writeVarInt(patterns.size());
        for (CraftingPattern pattern : patterns) {
            buf.writeItem(pattern.result());
            buf.writeVarInt(pattern.outputCount());
            List<ItemStack> nonEmpty = pattern.ingredients().stream().filter(s -> !s.isEmpty()).toList();
            buf.writeVarInt(nonEmpty.size());
            for (ItemStack ingredient : nonEmpty) {
                buf.writeItem(ingredient);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Client-only: cache the patterns and nudge any open ticker screen to refresh.
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.onPatternsSynced(networkId, patterns)));
        context.setPacketHandled(true);
    }
}
