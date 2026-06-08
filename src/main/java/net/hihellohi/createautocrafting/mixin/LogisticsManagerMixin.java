package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import net.hihellohi.createautocrafting.integration.CreateLogisticsBridge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(LogisticsManager.class)
public class LogisticsManagerMixin {

    @Inject(method = "createSummaryOfNetwork", at = @At("RETURN"), remap = false)
    private static void createautocrafting$mergeVirtualStock(UUID freqId,
                                                             CallbackInfoReturnable<InventorySummary> cir) {
        InventorySummary summary = cir.getReturnValue();
        if (summary != null) {
            CreateLogisticsBridge.mergeVirtualStock(freqId, summary);
        }
    }

    @Inject(method = "getStockOf", at = @At("RETURN"), remap = false)
    private static void createautocrafting$addVirtualStock(UUID freqId, ItemStack stack,
                                                             @Nullable IdentifiedInventory ignoredHandler,
                                                             CallbackInfoReturnable<Integer> cir) {
        int virtual = CreateLogisticsBridge.getVirtualStockCount(freqId, stack);
        if (virtual > 0) {
            cir.setReturnValue(cir.getReturnValue() + virtual);
        }
    }
}
