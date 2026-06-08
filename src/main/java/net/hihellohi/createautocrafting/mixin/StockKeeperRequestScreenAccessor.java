package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the private helpers of Create's request screen needed for our craftable handling. */
@Mixin(StockKeeperRequestScreen.class)
public interface StockKeeperRequestScreenAccessor {

    @Invoker(value = "getHoveredSlot", remap = false)
    Couple<Integer> createautocrafting$getHoveredSlot(int mouseX, int mouseY);

    @Invoker(value = "drawItemCount", remap = false)
    void createautocrafting$drawItemCount(GuiGraphics graphics, int count, int flag);
}
