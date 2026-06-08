package net.hihellohi.createautocrafting.client.screen;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.hihellohi.createautocrafting.network.CraftPlanRequestPacket;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * "How many?" selector shown when a craftable is middle-clicked in the stock ticker. It is Create's
 * own {@link ValueSettingsScreen} — the very control the Rotation Speed Controller uses — configured
 * with a <em>single</em> value bar (one board row) instead of two. Releasing a left-click on the bar
 * confirms the amount and asks the server for a crafting plan; Escape cancels back to the ticker.
 */
public class CraftAmountScreen extends ValueSettingsScreen {
    private static final int MIN = 1;
    private static final int MILESTONE = 16;

    @Nullable
    private final Screen parent;
    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int max;

    public CraftAmountScreen(@Nullable Screen parent, BlockPos tickerPos, ItemStack result, int max) {
        super(tickerPos, board(result, Math.max(MIN, max)),
                new ValueSettings(0, MIN), vs -> {}, 0);
        this.parent = parent;
        this.tickerPos = tickerPos;
        this.result = result;
        this.max = Math.max(MIN, max);
    }

    private static ValueSettingsBoard board(ItemStack result, int max) {
        ValueSettingsFormatter formatter =
                new ValueSettingsFormatter(vs -> Component.literal(String.valueOf(vs.value())));
        return new ValueSettingsBoard(
                result.getHoverName().copy(),
                max,
                MILESTONE,
                List.of(Component.translatable("gui.createautocrafting.craft_amount.row")),
                formatter);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) { // right click confirms the value the cursor is over
            ValueSettings picked = getClosestCoordinate((int) mouseX, (int) mouseY);
            confirm(picked == null ? MIN : picked.value());
            return true;
        }
        return true; // swallow other clicks; never run the base confirm (it'd send Create's packet)
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true; // never defer to the base confirm path
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font,
                Component.translatable("gui.createautocrafting.craft_amount.confirm_hint"),
                this.width / 2, this.height - 24, 0xFFFFFF);
    }

    private void confirm(int value) {
        int amount = Mth.clamp(value, MIN, max);
        ModPackets.sendToServer(new CraftPlanRequestPacket(tickerPos, result.copy(), amount));
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent); // Escape / close returns to the ticker without crafting
        }
    }
}
