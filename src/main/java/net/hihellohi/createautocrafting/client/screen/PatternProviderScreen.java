package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.menu.PatternProviderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pattern Provider GUI drawn from crafting_manager.png: two rows of eight blueprint pattern slots in
 * the top panel, the player inventory below. Dropping a configured Crafting Blueprint into a slot
 * teaches the provider that recipe.
 */
public class PatternProviderScreen extends AbstractContainerScreen<PatternProviderMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/gui/crafting_manager.png");
    // The drawn panel occupies (16,16)..(199,192) of the 256x256 sheet.
    private static final int TEX_U = 16;
    private static final int TEX_V = 16;

    public PatternProviderScreen(PatternProviderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 183;
        this.imageHeight = 176;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 11;
        this.inventoryLabelY = 88;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, TEX_U, TEX_V, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
