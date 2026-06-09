package net.hihellohi.createautocrafting.client.screen;

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
import org.jetbrains.annotations.Nullable;

/**
 * "How many?" selector for a craftable. A draggable value bar (it only moves while you hold a click
 * on it), with the supplied button texture used for a "Cancel" and a "Next" button. Next sends the
 * crafting-plan request; Cancel / Escape returns to the ticker.
 */
public class CraftAmountScreen extends Screen {
    private static final int MIN = 1;
    private static final ResourceLocation BUTTON =
            new ResourceLocation("createautocrafting", "textures/gui/button.png");

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 92;
    private static final int TRACK_H = 10;
    private static final int HANDLE_W = 6;

    @Nullable
    private final Screen parent;
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int max;
    private int amount = MIN;

    private int left;
    private int top;
    private int trackX;
    private int trackY;
    private int trackW;
    private boolean dragging;

    public CraftAmountScreen(@Nullable Screen parent, BlockPos tickerPos, ItemStack result, int max) {
        super(Component.translatable("gui.createautocrafting.craft_amount.title"));
        this.parent = parent;
        this.tickerPos = tickerPos;
        this.result = result;
        this.max = Math.max(MIN, max);
        this.amount = Math.min(amount, this.max);
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;
        trackX = left + 16;
        trackY = top + 44;
        trackW = PANEL_W - 32;

        Component cancelText = Component.translatable("gui.cancel");
        Component nextText = Component.translatable("gui.createautocrafting.craft_amount.next");
        int by = top + PANEL_H - 26;
        int nextW = font.width(nextText) + 16;
        int cancelW = font.width(cancelText) + 16;
        int nextX = left + PANEL_W - 12 - nextW;
        int cancelX = nextX - 6 - cancelW;
        addRenderableWidget(new TexButton(nextX, by, nextW, 20, nextText, b -> confirm()));
        addRenderableWidget(new TexButton(cancelX, by, cancelW, 20, cancelText, b -> onClose()));
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
    public boolean isPauseScreen() {
        return false;
    }

    private void setAmountFromMouse(double mouseX) {
        double t = (mouseX - trackX) / Math.max(1, trackW);
        amount = Mth.clamp((int) Math.round(MIN + t * (max - MIN)), MIN, max);
    }

    private boolean overTrack(double mouseX, double mouseY) {
        return mouseX >= trackX - HANDLE_W && mouseX <= trackX + trackW + HANDLE_W
                && mouseY >= trackY - 4 && mouseY <= trackY + TRACK_H + 4;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true; // a button handled it
        }
        if (button == 0 && overTrack(mouseX, mouseY)) {
            dragging = true;
            setAmountFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            setAmountFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        // Panel.
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFFC6C6C6);
        g.fill(left, top, left + PANEL_W, top + 1, 0xFFFFFFFF);
        g.fill(left, top, left + 1, top + PANEL_H, 0xFFFFFFFF);
        g.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF555555);
        g.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, 0xFF555555);

        // Header: item + name.
        g.renderItem(result, left + 12, top + 8);
        g.drawString(font, result.getHoverName(), left + 32, top + 12, 0x404040, false);

        // Value bar (drag-only).
        g.fill(trackX, trackY, trackX + trackW, trackY + TRACK_H, 0xFF373737);
        int fillW = (int) Math.round((double) (amount - MIN) / Math.max(1, max - MIN) * trackW);
        g.fill(trackX, trackY, trackX + fillW, trackY + TRACK_H, 0xFFB87333);
        int hx = trackX + Mth.clamp(fillW - HANDLE_W / 2, 0, trackW - HANDLE_W);
        g.fill(hx, trackY - 2, hx + HANDLE_W, trackY + TRACK_H + 2, 0xFFFFE0A0);
        g.fill(hx, trackY - 2, hx + HANDLE_W, trackY + TRACK_H + 2, 0x40000000);
        g.fill(hx, trackY - 2, hx + HANDLE_W, trackY + TRACK_H + 2, 0xFFE0B870);

        // Value number.
        Component amountText = Component.translatable("gui.createautocrafting.craft_amount.value", amount);
        g.drawCenteredString(font, amountText, left + PANEL_W / 2, top + 30, 0xFFFFFF);

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** A button drawn from the supplied frame texture (40x16 region at the top-left), 9-sliced. */
    private static class TexButton extends Button {
        private static final int SRC_W = 40;
        private static final int SRC_H = 16;
        private static final int C = 5; // corner inset preserved when scaling

        TexButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            // Nine-slice the 40x16 frame from the 64x64 texture into the button bounds.
            patch(g, x, y, C, C, 0, 0, C, C);
            patch(g, x + C, y, w - 2 * C, C, C, 0, SRC_W - 2 * C, C);
            patch(g, x + w - C, y, C, C, SRC_W - C, 0, C, C);
            patch(g, x, y + C, C, h - 2 * C, 0, C, C, SRC_H - 2 * C);
            patch(g, x + C, y + C, w - 2 * C, h - 2 * C, C, C, SRC_W - 2 * C, SRC_H - 2 * C);
            patch(g, x + w - C, y + C, C, h - 2 * C, SRC_W - C, C, C, SRC_H - 2 * C);
            patch(g, x, y + h - C, C, C, 0, SRC_H - C, C, C);
            patch(g, x + C, y + h - C, w - 2 * C, C, C, SRC_H - C, SRC_W - 2 * C, C);
            patch(g, x + w - C, y + h - C, C, C, SRC_W - C, SRC_H - C, C, C);

            int color = !active ? 0xA0A0A0 : (isHoveredOrFocused() ? 0x5A3A1A : 0x402810);
            var font = Minecraft.getInstance().font;
            g.drawCenteredString(font, getMessage(), x + w / 2, y + (h - 8) / 2, color);
        }

        private static void patch(GuiGraphics g, int dx, int dy, int dw, int dh, int sx, int sy, int sw, int sh) {
            if (dw <= 0 || dh <= 0) {
                return;
            }
            g.blit(BUTTON, dx, dy, dw, dh, sx, sy, sw, sh, 64, 64);
        }
    }
}
