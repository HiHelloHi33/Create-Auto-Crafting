package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.hihellohi.createautocrafting.api.PatternProviderHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Whenever a Pattern Provider's available stock is queried (i.e. while it participates in a
 * network summary), republish its patterns to the {@link net.hihellohi.createautocrafting.manager.PatternRegistry}.
 * This keeps the registry fresh and, crucially, repopulates it after a server restart without
 * requiring the player to re-open each provider.
 */
@Mixin(PackagerBlockEntity.class)
public class PackagerBlockEntityMixin {

    @Inject(method = "getAvailableItems()Lcom/simibubi/create/content/logistics/packager/InventorySummary;",
            at = @At("RETURN"), remap = false)
    private void createautocrafting$publishPatterns(CallbackInfoReturnable<InventorySummary> cir) {
        PackagerBlockEntity self = (PackagerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide || !PatternProviderHelper.isPatternProvider(self)) {
            return;
        }
        PatternProviderHelper.refreshRegistry(level, self.getBlockPos(), self);
    }
}
