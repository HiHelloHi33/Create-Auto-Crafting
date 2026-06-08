package net.hihellohi.createautocrafting.client.screen;

import net.hihellohi.createautocrafting.network.CraftRequestPacket;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.pattern.CraftingPlan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * AE2-style preview of a craft request: the crafting tree, pannable by dragging the mouse, with a
 * Start button (disabled unless the plan is satisfiable and a Logic Coil is on the network) and a
 * Cancel button. Confirming Start sends the {@link CraftRequestPacket}.
 */
public class CraftPlanScreen extends Screen {
    private static final int COL_W = 58;
    private static final int ROW_H = 26;
    private static final int OK_COLOR = 0xFF4CAF50;
    private static final int CRAFT_COLOR = 0xFF40A0FF;
    private static final int MISSING_COLOR = 0xFFE05050;
    private static final int LINE_COLOR = 0xFF707070;

    private final BlockPos tickerPos;
    private final ItemStack result;
    private final int amount;
    private final CraftingPlan plan;

    private final List<Placed> placed = new ArrayList<>();
    private double panX;
    private double panY;
    private int originX;
    private int originY;

    public CraftPlanScreen(BlockPos tickerPos, ItemStack result, int amount, CraftingPlan plan) {
        super(Component.translatable("gui.createautocrafting.plan.title"));
        this.tickerPos = tickerPos;
        this.result = result;
        this.amount = amount;
        this.plan = plan;
    }

    @Override
    protected void init() {
        placed.clear();
        layout(plan.root, 0, new int[]{0});
        this.originX = 36;
        this.originY = 48;

        int by = this.height - 30;
        Button start = Button.builder(Component.translatable("gui.createautocrafting.plan.start"), b -> start())
                .bounds(this.width / 2 - 104, by, 100, 20).build();
        start.active = plan.canStart();
        addRenderableWidget(start);
        addRenderableWidget(Button.builder(Component.translatable("gui.createautocrafting.plan.cancel"), b -> onClose())
                .bounds(this.width / 2 + 4, by, 100, 20).build());
    }

    /** Recursive tidy-ish layout: column by depth, row by leaf order, parents centred on children. */
    private int layout(CraftingPlan.CraftNode node, int depth, int[] leafRow) {
        int y;
        if (node.children.isEmpty()) {
            y = leafRow[0]++ * ROW_H;
        } else {
            int sum = 0;
            for (CraftingPlan.CraftNode child : node.children) {
                sum += layout(child, depth + 1, leafRow);
            }
            y = sum / node.children.size();
        }
        placed.add(new Placed(node, depth * COL_W, y));
        return y;
    }

    private void start() {
        if (plan.canStart()) {
            ModPackets.sendToServer(new CraftRequestPacket(tickerPos, result.copy(), amount));
        }
        onClose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (button == 0) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, this.width / 2, 12, 0xFFFFFF);

        Component subtitle = Component.translatable("gui.createautocrafting.plan.subtitle", amount, result.getHoverName());
        g.drawCenteredString(font, subtitle, this.width / 2, 24, 0xA0A0A0);

        // Tree, translated by pan.
        g.pose().pushPose();
        g.pose().translate(originX + panX, originY + panY, 0);
        for (Placed p : placed) {
            drawConnectors(g, p);
        }
        for (Placed p : placed) {
            drawNode(g, p);
        }
        g.pose().popPose();

        // Status line above the buttons.
        Component status;
        if (!plan.satisfiable) {
            status = Component.translatable("gui.createautocrafting.plan.missing");
        } else if (!plan.enoughCoils()) {
            status = Component.translatable("gui.createautocrafting.plan.no_coils", plan.coilsShort());
        } else {
            status = Component.translatable("gui.createautocrafting.plan.ready");
        }
        int statusColor = plan.canStart() ? 0xFF80FF80 : 0xFFFF6060;
        g.drawCenteredString(font, status, this.width / 2, this.height - 44, statusColor);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawConnectors(GuiGraphics g, Placed parent) {
        for (CraftingPlan.CraftNode child : parent.node.children) {
            Placed cp = find(child);
            if (cp == null) {
                continue;
            }
            int px = parent.x + 16;
            int py = parent.y + 8;
            int cx = cp.x;
            int cy = cp.y + 8;
            int midX = (px + cx) / 2;
            g.hLine(px, midX, py, LINE_COLOR);
            g.vLine(midX, Math.min(py, cy), Math.max(py, cy), LINE_COLOR);
            g.hLine(midX, cx, cy, LINE_COLOR);
        }
    }

    private void drawNode(GuiGraphics g, Placed p) {
        int color = p.node.isMissing() ? MISSING_COLOR : (p.node.craftable ? CRAFT_COLOR : OK_COLOR);
        // status border
        g.fill(p.x - 2, p.y - 2, p.x + 18, p.y + 18, color);
        g.fill(p.x - 1, p.y - 1, p.x + 17, p.y + 17, 0xFF202020);
        g.renderItem(p.node.stack, p.x, p.y);
        g.renderItemDecorations(font, p.node.stack, p.x, p.y);
        // needed (top-right) and available (below)
        g.drawString(font, "x" + p.node.needed, p.x + 20, p.y, 0xFFFFFF, false);
        Component avail = Component.translatable("gui.createautocrafting.plan.have", p.node.available);
        g.drawString(font, avail, p.x + 20, p.y + 9, p.node.isMissing() ? 0xFFFF6060 : 0xFF909090, false);
    }

    private Placed find(CraftingPlan.CraftNode node) {
        for (Placed p : placed) {
            if (p.node == node) {
                return p;
            }
        }
        return null;
    }

    private record Placed(CraftingPlan.CraftNode node, int x, int y) {}
}
