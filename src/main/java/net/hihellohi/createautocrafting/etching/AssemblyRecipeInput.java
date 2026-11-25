package net.hihellohi.createautocrafting.etching;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** The Circuit Assembler's contents: index 0 is the plate, the rest are the component slots. */
public record AssemblyRecipeInput(List<ItemStack> stacks) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
