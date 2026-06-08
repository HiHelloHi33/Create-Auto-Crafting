package net.hihellohi.createautocrafting.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.hihellohi.createautocrafting.client.screen.CraftingBlueprintScreen;
import net.hihellohi.createautocrafting.menu.CraftingBlueprintMenu;
import net.hihellohi.createautocrafting.network.BlueprintGhostPacket;
import net.hihellohi.createautocrafting.network.ModPackets;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a JEI item be dragged onto the Crafting Blueprint's 10 ghost slots (9 grid + result). On
 * drop, the slot is set optimistically on the client and a {@link BlueprintGhostPacket} persists it
 * server-side. Only item ingredients are accepted.
 */
public class BlueprintGhostHandler implements IGhostIngredientHandler<CraftingBlueprintScreen> {

    private static final int GHOST_SLOTS = 10; // 9 grid + 1 result

    @Override
    public <I> List<Target<I>> getTargetsTyped(CraftingBlueprintScreen gui,
                                               ITypedIngredient<I> ingredient, boolean doStart) {
        CraftingBlueprintMenu menu = gui.getMenu();
        ItemStack dragged = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (dragged.isEmpty() || menu.locked) {
            return List.of(); // only items map onto these slots; nothing when locked
        }
        List<Target<I>> targets = new ArrayList<>();
        for (int idx = 0; idx < GHOST_SLOTS && idx < menu.slots.size(); idx++) {
            final int slotIndex = idx;
            Slot slot = menu.slots.get(idx);
            int x = gui.getGuiLeft() + slot.x;
            int y = gui.getGuiTop() + slot.y;
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return new Rect2i(x, y, 16, 16);
                }

                @Override
                public void accept(I ingredient) {
                    menu.slots.get(slotIndex).set(dragged.copyWithCount(1)); // optimistic client set
                    ModPackets.sendToServer(new BlueprintGhostPacket(slotIndex, dragged));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
