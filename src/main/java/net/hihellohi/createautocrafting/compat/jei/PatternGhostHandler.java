package net.hihellohi.createautocrafting.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.hihellohi.createautocrafting.client.screen.PatternCardScreen;
import net.hihellohi.createautocrafting.menu.PatternCardMenu;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.hihellohi.createautocrafting.network.PatternGhostPacket;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a JEI item be dragged onto the Pattern Card's 10 ghost slots (9 grid + result). On drop, the
 * slot is set on the client immediately and a {@link PatternGhostPacket} saves it on the server.
 */
public class PatternGhostHandler implements IGhostIngredientHandler<PatternCardScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(PatternCardScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        PatternCardMenu menu = gui.getMenu();
        ItemStack dragged = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (dragged.isEmpty() || menu.locked) {
            return List.of();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (int idx = 0; idx < PatternCardMenu.GHOST_SLOTS; idx++) {
            final int slotIndex = idx;
            Slot slot = menu.slots.get(idx);
            Rect2i area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ignored) {
                    menu.setGhost(slotIndex, dragged);
                    ModPackets.sendToServer(new PatternGhostPacket(slotIndex, dragged));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
