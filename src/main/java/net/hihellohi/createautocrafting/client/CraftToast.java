package net.hihellohi.createautocrafting.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Top-right "achievement"-style popup announcing a finished craft. Drawn on the vanilla toast frame
 * with the crafted item's icon.
 */
public class CraftToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/toasts.png");
    private static final long DURATION = 5000L;

    private final ItemStack result;
    private final Component title;
    private final Component message;

    public CraftToast(ItemStack result, int amount) {
        this.result = result;
        this.title = Component.translatable("gui.createautocrafting.toast.title");
        this.message = Component.translatable("gui.createautocrafting.toast.msg", result.getHoverName());
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        graphics.blit(BACKGROUND, 0, 0, 0, 0, width(), height());
        Font font = toastComponent.getMinecraft().font;
        graphics.drawString(font, title, 30, 7, 0xFF8AE234, false);
        graphics.drawString(font, message, 30, 18, 0xFFFFFFFF, false);
        graphics.renderItem(result, 8, 8);
        return timeSinceLastVisible >= DURATION ? Visibility.HIDE : Visibility.SHOW;
    }
}
