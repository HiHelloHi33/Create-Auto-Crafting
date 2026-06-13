package net.hihellohi.createautocrafting.integration;

import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a {@link CraftingPattern} into the {@link CraftableBigItemStack} that Create's stock-ticker
 * request screen knows how to display and order. We synthesise a {@link ShapelessRecipe} on the fly
 * (ingredient order is irrelevant for stock-style requests) so Create's craftable machinery —
 * ingredient lookup, max-craftable maths, the amount control — all work unchanged.
 *
 * <p>In 1.21 the recipe id moved out of the recipe object into {@code RecipeHolder}; since these
 * synthetic recipes are never registered, the bare {@link ShapelessRecipe} is all Create needs.
 */
public final class RecipeSynth {
    private RecipeSynth() {}

    @Nullable
    public static CraftableBigItemStack toCraftable(CraftingPattern pattern) {
        ItemStack result = pattern.result();
        if (result.isEmpty()) {
            return null;
        }
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (ItemStack ingredient : pattern.ingredients()) {
            if (!ingredient.isEmpty()) {
                ingredients.add(Ingredient.of(ingredient));
            }
        }
        if (ingredients.isEmpty()) {
            return null;
        }
        ShapelessRecipe recipe = new ShapelessRecipe(
                "", CraftingBookCategory.MISC, result.copy(), ingredients);

        CraftableBigItemStack craftable = new CraftableBigItemStack(result.copy(), recipe);
        craftable.count = 0; // "available" count; the request screen recomputes the craftable amount
        return craftable;
    }
}
