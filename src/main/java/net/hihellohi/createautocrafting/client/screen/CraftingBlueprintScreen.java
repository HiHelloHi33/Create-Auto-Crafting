package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Editor for {@link CraftingBlueprintMenu}, drawn from the supplied {@code crafting_blueprint.png}
 * texture. The 3x3 ghost grid, result slot and player inventory line up with the texture's wells.
 * The title bar shows "Crafting Blueprint" on the left and a lock/unlock padlock toggle (from
 * {@code switches.png}) in the red box on the right. When unlocked (default) the pattern is fully
 * editable; when locked nothing can be changed.
 */
public class CraftingBlueprintScreen extends AbstractContainerScreen<CraftingBlueprintMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("createautocrafting", "textures/gui/crafting_blueprint.png");
    private static final ResourceLocation SWITCHES =
            new ResourceLocation("createautocrafting", "textures/gui/switches.png");

    // Switch sheet (32x16): two 16x16 padlocks side by side — left = locked (red, closed),
    // right = unlocked (green, open). Drawn into the red box in the title bar.
    private static final int SW_W = 16;
    private static final int SW_H = 16;
    private static final int SW_X = 155; // padlock's opaque 15x16 lands exactly on the red box
    private static final int SW_Y = 19;
    private static final int LOCKED_U = 0;
    private static final int UNLOCKED_U = 16;

    public CraftingBlueprintScreen(CraftingBlueprintMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 184;
        this.imageHeight = 191;
    }

    @Override
    protected void init() {
        super.init();
    }

    private void toggleLock() {
        menu.locked = !menu.locked;
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0); // 0 = toggle lock
        }
    }

    /** id encodes slot + count: 1 + slot*64 + (count-1); slots 0..8 = grid, 9 = result. */
    private void setSlotCount(int slotIndex, int count) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1 + slotIndex * 64 + (count - 1));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverSwitch(mouseX, mouseY)) {
            toggleLock();
            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverSwitch(double mouseX, double mouseY) {
        int x = leftPos + SW_X;
        int y = topPos + SW_Y;
        return mouseX >= x && mouseX < x + SW_W && mouseY >= y && mouseY < y + SW_H;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && hovered.hasItem()) {
            int idx = menu.slots.indexOf(hovered);
            boolean editable = !menu.locked && idx >= 0 && idx < 10; // 0..8 grid, 9 result
            if (editable) {
                int newCount = Mth.clamp(hovered.getItem().getCount() + (delta > 0 ? 1 : -1), 1, 64);
                ItemStack shown = hovered.getItem().copy(); // optimistic client display
                shown.setCount(newCount);
                hovered.set(shown);
                setSlotCount(idx, newCount);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title bar text (coordinates are local to the GUI's top-left here).
        g.drawString(font, title, 8, 4, 0xFFFFFF, true);

        // Lock/unlock padlock, drawn into the red box. Left sprite = locked, right = unlocked.
        int u = menu.locked ? LOCKED_U : UNLOCKED_U;
        g.blit(SWITCHES, SW_X, SW_Y, u, 0, SW_W, SW_H, 32, 16);
    }
}
