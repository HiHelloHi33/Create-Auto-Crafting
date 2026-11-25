package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.hihellohi.createautocrafting.blocks.CraftingDistributorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create picks a Packager's tray by checking {@code AllBlocks.PACKAGER}; anything else gets the
 * Repackager's tray. The Crafting Distributor is a Packager, so give it the regular one.
 */
@Mixin(PackagerRenderer.class)
public class PackagerRendererMixin {

    @Inject(method = "getTrayModel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createautocrafting$distributorTray(BlockState state, CallbackInfoReturnable<PartialModel> cir) {
        if (state.getBlock() instanceof CraftingDistributorBlock) {
            cir.setReturnValue(AllPartialModels.PACKAGER_TRAY_REGULAR);
        }
    }
}
