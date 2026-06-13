package net.hihellohi.createautocrafting.client.screen;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.hihellohi.createautocrafting.network.CraftPlanRequestPacket;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * "How many?" selector — Create's own value-settings board (the very control the Rotation Speed
 * Controller uses), one bar, plus a Cancel and a Next button drawn from our button texture. The bar
 * works exactly like Create's: the value follows the cursor over it. Next confirms the value and
 * asks the server for a crafting plan; Cancel / Escape returns to the ticker.
 */
public class CraftAmountScreen extends ValueSettingsScreen {
    private static final int MIN = 1;
    private static final int MILESTONE = 16;
    private static final ResourceLocation BUTTON =
            new ResourceLocation("createautocrafting", "textures/gui/button.png");

    @Nullable
    private final Screen parent;
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int max;
    private int amount = MIN;

    private TexButton cancelButton;
    private TexButton nextButton;
    private boolean dragging;

    public CraftAmountScreen(@Nullable Screen parent, BlockPos tickerPos, ItemStack result, int max) {
        super(tickerPos, board(result, Math.max(MIN, max)), new ValueSettings(0, MIN), vs -> {}, 0);
        this.parent = parent;
        this.tickerPos = tickerPos;
        this.result = result;
        this.max = Math.max(MIN, max);
    }

    private static ValueSettingsBoard board(ItemStack result, int max) {
        ValueSettingsFormatter formatter =
                new ValueSettingsFormatter(vs -> Component.literal(String.valueOf(vs.value())));
        return new ValueSettingsBoard(result.getHoverName().copy(), max, MILESTONE,
                List.of(Component.translatable("gui.createautocrafting.craft_amount.row")), formatter);
    }

    @Override
    protected void init() {
        super.init();
        Component cancelText = Component.translatable("gui.cancel");
        Component nextText = Component.translatable("gui.createautocrafting.craft_amount.next");
        int cancelW = font.width(cancelText) + 16;
        int nextW = font.width(nextText) + 16;
        int gap = 6;
        int total = cancelW + gap + nextW;
        int y = this.height / 2 + 46;
        int cancelX = (this.width - total) / 2;
        int nextX = cancelX + cancelW + gap;
        cancelButton = new TexButton(cancelX, y, cancelW, 20, cancelText, b -> onClose());
        nextButton = new TexButton(nextX, y, nextW, 20, nextText, b -> confirm());
        addRenderableWidget(cancelButton);
        addRenderableWidget(nextButton);
    }

    private boolean overButton(double mouseX, double mouseY) {
        return (cancelButton != null && cancelButton.isMouseOver(mouseX, mouseY))
                || (nextButton != null && nextButton.isMouseOver(mouseX, mouseY));
    }

    /** Updates {@code amount} from the cursor — only used while actively dragging the bar. */
    private void pickFromMouse(double mouseX, double mouseY) {
        ValueSettings v = getClosestCoordinate((int) mouseX, (int) mouseY);
        if (v != null) {
            amount = Mth.clamp(v.value(), MIN, max);
        }
    }

    private boolean overBar(double mouseX, double mouseY) {
        return !overButton(mouseX, mouseY) && Math.abs(mouseY - this.height / 2) < 44;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true; // a button handled it
        }
        if (button == 0 && overBar(mouseX, mouseY)) {
            dragging = true;
            pickFromMouse(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            pickFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** Extra darkening over Create's default backdrop so the picker pops more. Raise alpha to dim further. */
    private static final int EXTRA_DIM = 0x99000000;

    @Override
    protected void renderWindowBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWindowBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, EXTRA_DIM);
    }

    @Override
    protected void renderWindow(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // The bar always shows the picked value; it only changes while you click-and-drag it.
        Vec2 p = getCoordinateOfValue(0, amount);
        super.renderWindow(g, (int) p.x, (int) p.y, partialTick);
    }

    private void confirm() {
        ModPackets.sendToServer(new CraftPlanRequestPacket(tickerPos, result.copy(), Mth.clamp(amount, MIN, max)));
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        // Don't run Create's release-to-apply (it would send its own packet); confirm is the button.
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** A button drawn from our frame texture (40x16 region at the top-left of a 64x64 png), 9-sliced. */
    private static class TexButton extends Button {
        private static final int SRC_W = 40;
        private static final int SRC_H = 16;
        private static final int C = 5;

        TexButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            patch(g, x, y, C, C, 0, 0, C, C);
            patch(g, x + C, y, w - 2 * C, C, C, 0, SRC_W - 2 * C, C);
            patch(g, x + w - C, y, C, C, SRC_W - C, 0, C, C);
            patch(g, x, y + C, C, h - 2 * C, 0, C, C, SRC_H - 2 * C);
            patch(g, x + C, y + C, w - 2 * C, h - 2 * C, C, C, SRC_W - 2 * C, SRC_H - 2 * C);
            patch(g, x + w - C, y + C, C, h - 2 * C, SRC_W - C, C, C, SRC_H - 2 * C);
            patch(g, x, y + h - C, C, C, 0, SRC_H - C, C, C);
            patch(g, x + C, y + h - C, w - 2 * C, C, C, SRC_H - C, SRC_W - 2 * C, C);
            patch(g, x + w - C, y + h - C, C, C, SRC_W - C, SRC_H - C, C, C);

            // Normal Minecraft button text: white with drop shadow (grey when disabled).
            int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, color);
        }

        private static void patch(GuiGraphics g, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh) {
            if (dw <= 0 || dh <= 0) {
                return;
            }
            g.blit(BUTTON, dx, dy, dw, dh, sx, sy, sw, sh, 64, 64);
        }
    }
}
