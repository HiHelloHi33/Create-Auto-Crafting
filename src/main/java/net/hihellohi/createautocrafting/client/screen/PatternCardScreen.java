package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.menu.PatternCardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Editor for a Pattern Card, drawn from {@code textures/gui/pattern_card.png}. The grid and result slots
 * are ghost slots, so:
 * <ul>
 *   <li>click a slot with an item on your cursor to copy it in (the cursor item is never used up);</li>
 *   <li><b>click and drag</b> across several slots to copy it into every slot you pass over;</li>
 *   <li>click with an empty cursor to clear a slot; scroll over a slot to change its amount.</li>
 * </ul>
 * The padlock in the corner locks the card against changes.
 */
public class PatternCardScreen extends AbstractContainerScreen<PatternCardMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/gui/pattern_card.png");
    private static final ResourceLocation SWITCHES =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/gui/switches.png");

    // switches.png (32x16): left = locked padlock, right = unlocked. Drawn into the red box in the texture.
    private static final int SW_SIZE = 16;
    private static final int SW_X = 155;
    private static final int SW_Y = 19;

    /** The ghost slot most recently painted by a drag, so a drag paints each slot once. */
    private int lastPainted = -1;

    public PatternCardScreen(PatternCardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 184;
        this.imageHeight = 191;
        this.titleLabelX = 8;
        this.titleLabelY = 4;
    }

    private void click(int slotIndex) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0, ClickType.PICKUP, minecraft.player);
        }
    }

    private void button(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** The ghost slot under the mouse, or -1. */
    private int ghostSlotAt(double mouseX, double mouseY) {
        for (int i = 0; i < PatternCardMenu.GHOST_SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            if (mouseX >= leftPos + slot.x && mouseX < leftPos + slot.x + 16
                    && mouseY >= topPos + slot.y && mouseY < topPos + slot.y + 16) {
                return i;
            }
        }
        return -1;
    }

    private boolean overLock(double mouseX, double mouseY) {
        return mouseX >= leftPos + SW_X && mouseX < leftPos + SW_X + SW_SIZE
                && mouseY >= topPos + SW_Y && mouseY < topPos + SW_Y + SW_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && overLock(mouseX, mouseY)) {
            menu.locked = !menu.locked;
            button(0);
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        int ghost = ghostSlotAt(mouseX, mouseY);
        if (ghost >= 0 && (mouseButton == 0 || mouseButton == 1)) {
            if (!menu.locked) {
                click(ghost);
                lastPainted = ghost;
            }
            return true; // ghost slots never use vanilla's pick-up/split-drag handling
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
        if ((mouseButton == 0 || mouseButton == 1) && !menu.getCarried().isEmpty() && !menu.locked) {
            int ghost = ghostSlotAt(mouseX, mouseY);
            if (ghost >= 0 && ghost != lastPainted) {
                click(ghost); // copies the carried item into this slot, just like the first click did
                lastPainted = ghost;
            }
            if (ghost >= 0 || lastPainted >= 0) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, mouseButton, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        lastPainted = -1;
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int ghost = ghostSlotAt(mouseX, mouseY);
        if (ghost >= 0 && !menu.locked) {
            Slot slot = menu.slots.get(ghost);
            if (slot.hasItem()) {
                int count = Mth.clamp(slot.getItem().getCount() + (scrollY > 0 ? 1 : -1), 1, 64);
                ItemStack shown = slot.getItem().copyWithCount(count); // optimistic display
                slot.set(shown);
                button(1 + ghost * 64 + (count - 1));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.blit(SWITCHES, SW_X, SW_Y, menu.locked ? 0 : SW_SIZE, 0, SW_SIZE, SW_SIZE, 32, 16);
    }
}
