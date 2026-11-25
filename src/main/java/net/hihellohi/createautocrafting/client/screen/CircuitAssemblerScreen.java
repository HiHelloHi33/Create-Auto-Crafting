package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.blocks.CircuitAssemblerBlockEntity;
import net.hihellohi.createautocrafting.menu.CircuitAssemblerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Circuit Assembler GUI, drawn from textures/gui/circuit_assembler.png: the plate slot in the middle, a ring of five
 * component slots that show a faint ghost of what they take, and the output slot with a progress arrow.
 */
public class CircuitAssemblerScreen extends AbstractContainerScreen<CircuitAssemblerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/gui/circuit_assembler.png");
    private static final ResourceLocation PLATE_GHOST =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/item/components/lt1_etched_plate.png");

    /** Ghost overlays for the component slots, in COMPONENT_ITEMS order (resistor, inductor, diode, capacitor, transistor). */
    private static final ResourceLocation[] GHOST_TEXTURES = {
            ghostTexture("lresistor"), ghostTexture("linductor"), ghostTexture("ldiode"),
            ghostTexture("lcapacitor"), ghostTexture("ltransistor")};

    private static final ResourceLocation SWITCH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/gui/circuit_assembler_sw.png");

    /** On/off switch: sits on the red square in the texture (20x16; the SW sheet has green on top, red below). */
    private static final int SW_X = 57, SW_Y = 26, SW_W = 20, SW_H = 16;
    /** The parchment tag in the top right: tier readout and scroll area. */
    private static final int TAG_X = 126, TAG_Y = 28, TAG_W = 41, TAG_H = 17;

    /** Progress arrow pixels (x offset from ARROW_X, row from ARROW_Y): start column and end column (exclusive) per row. */
    private static final int ARROW_X = 130, ARROW_Y = 66;
    private static final int[][] ARROW_ROWS = {
            {6, 8}, {6, 9}, {6, 10}, {6, 11}, {0, 12}, {0, 13}, {0, 14}, {0, 14}, {0, 13}, {0, 12},
            {6, 11}, {6, 10}, {6, 9}, {6, 8}};

    private static ResourceLocation ghostTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath("createautocrafting", "textures/item/components/" + name + ".png");
    }

    public CircuitAssemblerScreen(CircuitAssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
        this.titleLabelX = 8;
        this.titleLabelY = 4;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 127;
    }

    private boolean overSwitch(double mx, double my) {
        return mx >= leftPos + SW_X && mx < leftPos + SW_X + SW_W && my >= topPos + SW_Y && my < topPos + SW_Y + SW_H;
    }

    private boolean overTag(double mx, double my) {
        return mx >= leftPos + TAG_X && mx < leftPos + TAG_X + TAG_W && my >= topPos + TAG_Y && my < topPos + TAG_Y + TAG_H;
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overSwitch(mx, my)) {
            click(CircuitAssemblerMenu.BUTTON_TOGGLE);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (overTag(mx, my) && scrollY != 0) {
            click(scrollY > 0 ? CircuitAssemblerMenu.BUTTON_TIER_DOWN : CircuitAssemblerMenu.BUTTON_TIER_UP);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.4f));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);

        // On/off switch over the red square.
        g.blit(SWITCH_TEXTURE, leftPos + SW_X, topPos + SW_Y, 0, menu.isActive() ? 0 : SW_H, SW_W, SW_H, 20, 32);

        // Ghost of the T1 etched plate in the empty plate slot.
        Slot plate = menu.slots.get(0);
        if (!plate.hasItem()) {
            g.blit(PLATE_GHOST, leftPos + plate.x, topPos + plate.y, 0, 0, 16, 16, 16, 16);
            g.fill(leftPos + plate.x, topPos + plate.y, leftPos + plate.x + 16, topPos + plate.y + 16, 0xA08B8B8B);
        }
        for (int i = 0; i < CircuitAssemblerBlockEntity.COMPONENT_COUNT; i++) {
            Slot s = menu.slots.get(1 + i);
            if (!s.hasItem()) {
                g.blit(GHOST_TEXTURES[i], leftPos + s.x, topPos + s.y, 0, 0, 16, 16, 16, 16);
            }
        }

        // Progress arrow: lighten the arrow's pixels left to right.
        int filled = (int) Math.ceil(14 * menu.progress());
        for (int row = 0; row < ARROW_ROWS.length; row++) {
            int from = ARROW_ROWS[row][0];
            int to = Math.min(ARROW_ROWS[row][1], filled);
            if (to > from) {
                g.fill(leftPos + ARROW_X + from, topPos + ARROW_Y + row,
                        leftPos + ARROW_X + to, topPos + ARROW_Y + row + 1, 0xFFE8B070);
            }
        }

        // Tier readout on the parchment tag: name on the left, the circuit on the right.
        int tier = menu.tier();
        ItemStack circuit = new ItemStack(CircuitAssemblerBlockEntity.TIER_CIRCUITS.get(
                Math.max(0, Math.min(CircuitAssemblerBlockEntity.TIER_CIRCUITS.size() - 1, tier))).get());
        g.pose().pushPose();
        g.pose().translate(leftPos + 152, topPos + 30, 0);
        g.pose().scale(0.75f, 0.75f, 1f);
        g.renderItem(circuit, 0, 0);
        g.pose().popPose();

        String name = Component.translatable("gui.createautocrafting.circuit_assembler.tier." + (tier + 1)).getString();
        int sp = name.indexOf(' ');
        String[] lines = sp < 0 ? new String[]{name} : new String[]{name.substring(0, sp), name.substring(sp + 1)};
        float scale = 0.6f;
        int lineH = 5;
        float y = topPos + 36.5f - lines.length * lineH / 2f;
        for (String line : lines) {
            g.pose().pushPose();
            g.pose().translate(leftPos + 135, y, 0);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, line, 0, 0, 0x4A3322, false);
            g.pose().popPose();
            y += lineH;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (overSwitch(mouseX, mouseY)) {
            g.renderTooltip(font, Component.translatable(menu.isActive()
                    ? "gui.createautocrafting.circuit_assembler.on"
                    : "gui.createautocrafting.circuit_assembler.off"), mouseX, mouseY);
        } else {
            renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
