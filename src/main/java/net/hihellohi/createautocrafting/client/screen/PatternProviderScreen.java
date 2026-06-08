package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.menu.PatternProviderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Flat-drawn Pattern Provider GUI — the frogport-style panel without frogport functionality.
 * Top row holds blueprint pattern slots; below is the player inventory.
 */
public class PatternProviderScreen extends AbstractContainerScreen<PatternProviderMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF373737;
    private static final int PANEL_HL = 0xFFFFFFFF;
    private static final int PANEL_SH = 0xFF555555;

    public PatternProviderScreen(PatternProviderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        g.fill(x, y, x + imageWidth, y + 1, PANEL_HL);
        g.fill(x, y, x + 1, y + imageHeight, PANEL_HL);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_SH);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_SH);

        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, PANEL_SH);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, PANEL_DARK);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
