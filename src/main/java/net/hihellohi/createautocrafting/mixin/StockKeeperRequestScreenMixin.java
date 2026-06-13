package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.createmod.catnip.data.Couple;
import net.hihellohi.createautocrafting.client.screen.CraftAmountScreen;
import net.hihellohi.createautocrafting.integration.RecipeSynth;
import net.hihellohi.createautocrafting.manager.ClientPatternCache;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Surfaces our patterns as craftable in Create's request screen as a SINGLE entry per item:
 * <ul>
 *   <li>On every refresh we merge craftables into {@code currentItemSource} — replacing the matching
 *       real-stock entry with a {@link CraftableBigItemStack} that keeps its count, or adding a new
 *       zero-stock entry. Doing it here (not in the stock packet) makes it work on dedicated servers
 *       regardless of when {@code PatternSyncPacket} arrives, and merges craftable + stock into one
 *       row instead of two.</li>
 *   <li>Count rendering: a craftable with stock shows its count; with no stock it shows a {@code +}.</li>
 *   <li>Middle-clicking a craftable opens the amount slider.</li>
 * </ul>
 */
@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

    @Inject(method = "refreshSearchResults(Z)V", at = @At("HEAD"), remap = false)
    private void createautocrafting$mergeCraftables(boolean fromSearch, CallbackInfo ci) {
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        List<List<BigItemStack>> source = self.currentItemSource;
        if (source == null) {
            return;
        }
        UUID networkId = networkId(self);
        if (networkId == null) {
            return;
        }
        List<CraftingPattern> patterns = ClientPatternCache.get(networkId);
        if (patterns.isEmpty()) {
            return;
        }

        for (CraftingPattern pattern : patterns) {
            ItemStack result = pattern.result();
            CraftableBigItemStack craftable = RecipeSynth.toCraftable(pattern);
            if (craftable == null) {
                continue;
            }

            // Convert the FIRST matching entry into our craftable and delete any other matches, so
            // the item shows exactly once (Create lists an item in every matching category sublist).
            boolean placed = false;
            for (List<BigItemStack> category : source) {
                for (int i = 0; i < category.size(); i++) {
                    BigItemStack entry = category.get(i);
                    if (entry.stack == null || !ItemStack.isSameItemSameComponents(entry.stack, result)) {
                        continue;
                    }
                    if (entry instanceof CraftableBigItemStack) {
                        placed = true; // already ours from a prior pass; keep this one
                        continue;
                    }
                    if (!placed) {
                        craftable.count = entry.count; // keep the real stock count on the one entry
                        category.set(i, craftable);
                        placed = true;
                    } else {
                        category.remove(i);
                        i--; // duplicate removed; re-check this index
                    }
                }
            }

            if (!placed) {
                craftable.count = 0; // craftable only, no stock -> shows "+"
                if (source.isEmpty()) {
                    source.add(new ArrayList<>());
                }
                source.get(0).add(craftable);
            }
        }
    }

    @Redirect(
            method = "renderItemEntry(Lnet/minecraft/client/gui/GuiGraphics;FLcom/simibubi/create/content/logistics/BigItemStack;ZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/logistics/stockTicker/StockKeeperRequestScreen;drawItemCount(Lnet/minecraft/client/gui/GuiGraphics;II)V"),
            remap = false)
    private void createautocrafting$drawCraftableCount(StockKeeperRequestScreen self, GuiGraphics graphics,
                                                       int count, int flag,
                                                       GuiGraphics outerGraphics, float partialTicks,
                                                       BigItemStack entry, boolean b1, boolean b2) {
        StockKeeperRequestScreenAccessor accessor = (StockKeeperRequestScreenAccessor) self;
        if (entry instanceof CraftableBigItemStack) {
            // Show the real stock count when there is some (suppress a misleading "0"), and ALWAYS
            // draw a "+" badge in the corner to mark the entry as craftable.
            if (count > 0) {
                accessor.createautocrafting$drawItemCount(graphics, count, flag);
            }
            graphics.drawString(Minecraft.getInstance().font, "+", 0, 8, 0xFF55FF55, true);
        } else {
            accessor.createautocrafting$drawItemCount(graphics, count, flag);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void createautocrafting$middleClickCraft(double mouseX, double mouseY, int button,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (button != 2) {
            return; // only middle-click
        }
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        Couple<Integer> slot = ((StockKeeperRequestScreenAccessor) self)
                .createautocrafting$getHoveredSlot((int) mouseX, (int) mouseY);
        if (slot == null) {
            return;
        }
        Integer category = slot.getFirst();
        Integer index = slot.getSecond();
        if (category == null || index == null) {
            return;
        }
        List<List<BigItemStack>> items = self.displayedItems;
        if (items == null || category < 0 || category >= items.size()) {
            return;
        }
        List<BigItemStack> row = items.get(category);
        if (index < 0 || index >= row.size()) {
            return;
        }
        if (row.get(index) instanceof CraftableBigItemStack craftable) {
            StockKeeperRequestMenu menu = self.getMenu();
            if (menu.contentHolder == null) {
                return;
            }
            BlockPos tickerPos = menu.contentHolder.getBlockPos();
            Minecraft.getInstance().setScreen(
                    new CraftAmountScreen(self, tickerPos, craftable.stack.copy(), 256));
            cir.setReturnValue(true);
        }
    }

    private static UUID networkId(StockKeeperRequestScreen self) {
        StockKeeperRequestMenu menu = self.getMenu();
        if (menu == null || menu.contentHolder == null || menu.contentHolder.behaviour == null) {
            return null;
        }
        return menu.contentHolder.behaviour.freqId;
    }
}
