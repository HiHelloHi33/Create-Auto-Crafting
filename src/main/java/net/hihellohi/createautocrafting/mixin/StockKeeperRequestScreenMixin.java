package net.hihellohi.createautocrafting.mixin;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.createmod.catnip.data.Couple;
import net.hihellohi.createautocrafting.client.BrassIcons;
import net.hihellohi.createautocrafting.client.CraftableFilter;
import net.hihellohi.createautocrafting.client.screen.CraftAmountScreen;
import net.hihellohi.createautocrafting.integration.RecipeSynth;
import net.hihellohi.createautocrafting.manager.ClientPatternCache;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
 *   <li>Count rendering: a craftable shows its stock count (if any) plus a brass {@code +} badge.</li>
 *   <li>Middle-clicking a craftable opens the amount slider.</li>
 *   <li>A small filter button beside the search bar switches between craftable, non-craftable and
 *       all entries ({@link CraftableFilter}).</li>
 * </ul>
 */
@Mixin(StockKeeperRequestScreen.class)
public abstract class StockKeeperRequestScreenMixin {

    /** The real item source while a filtered copy is installed for the duration of a refresh. */
    @Unique
    private List<List<BigItemStack>> createautocrafting$unfilteredSource;

    @Inject(method = "refreshSearchResults(Z)V", at = @At("HEAD"), remap = false)
    private void createautocrafting$beforeRefresh(boolean fromSearch, CallbackInfo ci) {
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        List<List<BigItemStack>> source = self.currentItemSource;
        if (source == null) {
            return;
        }
        createautocrafting$mergeCraftables(self, source);

        // Filter: Create builds the displayed rows (and their layout) straight from currentItemSource,
        // so hand it a filtered copy for the duration of the refresh and put the real one back after.
        CraftableFilter filter = CraftableFilter.current();
        if (filter != CraftableFilter.BOTH) {
            List<List<BigItemStack>> filtered = new ArrayList<>(source.size());
            for (List<BigItemStack> category : source) {
                List<BigItemStack> kept = new ArrayList<>();
                for (BigItemStack entry : category) {
                    if (filter.shows(entry instanceof CraftableBigItemStack)) {
                        kept.add(entry);
                    }
                }
                filtered.add(kept);
            }
            createautocrafting$unfilteredSource = source;
            ((StockKeeperRequestScreenAccessor) self).createautocrafting$setCurrentItemSource(filtered);
        }
    }

    @Inject(method = "refreshSearchResults(Z)V", at = @At("RETURN"), remap = false)
    private void createautocrafting$afterRefresh(boolean fromSearch, CallbackInfo ci) {
        if (createautocrafting$unfilteredSource != null) {
            ((StockKeeperRequestScreenAccessor) (Object) this)
                    .createautocrafting$setCurrentItemSource(createautocrafting$unfilteredSource);
            createautocrafting$unfilteredSource = null;
        }
    }

    @Unique
    private static void createautocrafting$mergeCraftables(StockKeeperRequestScreen self,
                                                           List<List<BigItemStack>> source) {
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
            // draw a brass "+" badge in the corner to mark the entry as craftable.
            if (count > 0) {
                accessor.createautocrafting$drawItemCount(graphics, count, flag);
            }
            BrassIcons.plus(graphics, 0, 9);
        } else {
            accessor.createautocrafting$drawItemCount(graphics, count, flag);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void createautocrafting$middleClickCraft(double mouseX, double mouseY, int button,
                                                     CallbackInfoReturnable<Boolean> cir) {
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        if (button == 0 && createautocrafting$overFilterButton(self, mouseX, mouseY)) {
            CraftableFilter.cycle();
            self.refreshSearchNextTick = true;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
            return;
        }
        if (button != 2) {
            return; // the rest is middle-click only
        }
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

    // ---- filter button ------------------------------------------------------------------

    @Unique private static final int FILTER_SIZE = 18;

    /** Left edge of the filter button: the free space left of the search bar (JEI's sync button sits right of it). */
    @Unique
    private static int createautocrafting$filterX(StockKeeperRequestScreen self) {
        return ((StockKeeperRequestScreenAccessor) (Object) self).createautocrafting$getItemsX() - 17;
    }

    @Unique
    private static int createautocrafting$filterY(StockKeeperRequestScreen self) {
        return ((StockKeeperRequestScreenAccessor) (Object) self).createautocrafting$getItemsY() - 15;
    }

    @Unique
    private static boolean createautocrafting$overFilterButton(StockKeeperRequestScreen self, double mx, double my) {
        int x = createautocrafting$filterX(self);
        int y = createautocrafting$filterY(self);
        return mx >= x && mx < x + FILTER_SIZE && my >= y && my < y + FILTER_SIZE;
    }

    @Inject(method = "renderForeground", at = @At("TAIL"), remap = false)
    private void createautocrafting$renderFilterButton(GuiGraphics g, int mouseX, int mouseY, float partialTicks,
                                                       CallbackInfo ci) {
        StockKeeperRequestScreen self = (StockKeeperRequestScreen) (Object) this;
        int x = createautocrafting$filterX(self);
        int y = createautocrafting$filterY(self);
        boolean hovered = createautocrafting$overFilterButton(self, mouseX, mouseY);
        CraftableFilter filter = CraftableFilter.current();

        // Button: dark outline, brass-brown face, lighter when hovered.
        g.fill(x, y, x + FILTER_SIZE, y + FILTER_SIZE, 0xFF2E1F12);
        g.fill(x + 1, y + 1, x + FILTER_SIZE - 1, y + FILTER_SIZE - 1, hovered ? 0xFFB98A4E : 0xFF8F6A3F);
        g.fill(x + 1, y + 1, x + FILTER_SIZE - 1, y + 2, 0xFFD9B070);
        g.fill(x + 1, y + 1, x + 2, y + FILTER_SIZE - 1, 0xFFD9B070);

        // Icon: brass plus = craftable only, plain chest = stocked only, both = chest with a plus.
        net.minecraft.world.item.ItemStack chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);
        switch (filter) {
            case CRAFTABLE -> BrassIcons.plus(g, x + (FILTER_SIZE - BrassIcons.PLUS_SIZE) / 2,
                    y + (FILTER_SIZE - BrassIcons.PLUS_SIZE) / 2);
            case NON_CRAFTABLE -> g.renderItem(chest, x + 1, y + 1);
            case BOTH -> {
                g.renderItem(chest, x + 1, y + 1);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                BrassIcons.plus(g, x + FILTER_SIZE - BrassIcons.PLUS_SIZE - 1, y + FILTER_SIZE - BrassIcons.PLUS_SIZE - 1);
                g.pose().popPose();
            }
        }

        if (hovered) {
            Minecraft mc = Minecraft.getInstance();
            g.renderComponentTooltip(mc.font, List.of(filter.label(), filter.description()), mouseX, mouseY);
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
