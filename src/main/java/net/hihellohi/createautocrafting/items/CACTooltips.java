package net.hihellohi.createautocrafting.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Create-style "Press [Shift] to display info" item tooltips. Items that don't ship a Ponder scene
 * call {@link #appendShiftInfo} from their {@code appendHoverText}: while Shift is held the given
 * translatable description lines are shown, otherwise a single dimmed hint line is shown. The hint
 * is built in code so the {@code [Shift]} key can be highlighted; the description lines stay
 * translatable so they live in the lang file with everything else.
 */
public final class CACTooltips {
    private CACTooltips() {}

    public static void appendShiftInfo(List<Component> tooltip, String... descriptionKeys) {
        if (Screen.hasShiftDown()) {
            for (String key : descriptionKeys) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(holdShiftHint());
        }
    }

    private static Component holdShiftHint() {
        return Component.literal("Press ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("[Shift]").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" to display info").withStyle(ChatFormatting.DARK_GRAY));
    }
}
