package net.hihellohi.createautocrafting.etching;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What is in the Etching Factory's basin: the plate being dipped (the acid is checked separately). */
public record EtchingRecipeInput(ItemStack base) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return base;
    }

    @Override
    public int size() {
        return 1;
    }
}
