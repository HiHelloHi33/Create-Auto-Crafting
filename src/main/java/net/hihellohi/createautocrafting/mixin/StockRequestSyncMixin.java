package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.stockTicker.LogisticalStockRequestPacket;
import com.simibubi.create.content.logistics.stockTicker.StockCheckingBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import net.hihellohi.createautocrafting.manager.PatternRegistry;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.network.PatternSyncPacket;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

/**
 * When the server handles a player's request for a Stock Ticker's contents, also send that player
 * the craftable patterns available on the ticker's network. This is the multiplayer bridge: the
 * client caches them ({@code ClientPatternCache}) so the request screen can show them as craftable.
 */
@Mixin(LogisticalStockRequestPacket.class)
public class StockRequestSyncMixin {

    @Inject(
            method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/logistics/stockTicker/StockCheckingBlockEntity;)V",
            at = @At("TAIL"),
            remap = false)
    private void createautocrafting$syncPatterns(ServerPlayer player, StockCheckingBlockEntity be, CallbackInfo ci) {
        if (!(be instanceof StockTickerBlockEntity ticker) || ticker.behaviour == null) {
            return;
        }
        UUID networkId = ticker.behaviour.freqId;
        if (networkId == null || !(ticker.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<CraftingPattern> patterns = PatternRegistry.getPatternsForNetwork(level, networkId);
        ModPackets.sendToPlayer(new PatternSyncPacket(networkId, patterns), player);
    }
}
