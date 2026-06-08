package net.hihellohi.createautocrafting.integration;

import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import net.hihellohi.createautocrafting.pattern.CraftingPattern;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Turns a {@link CraftingPattern} into the {@link CraftableBigItemStack} that Create's stock-ticker
 * request screen knows how to display and order. We synthesise a {@link ShapelessRecipe} on the fly
 * (ingredient order is irrelevant for stock-style requests) so Create's craftable machinery —
 * ingredient lookup, max-craftable maths, the amount control — all work unchanged.
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
        ResourceLocation id = new ResourceLocation("createautocrafting",
                "pattern/" + safePath(ForgeRegistries.ITEMS.getKey(result.getItem())));
        ShapelessRecipe recipe = new ShapelessRecipe(
                id, "", CraftingBookCategory.MISC, result.copy(), ingredients);

        CraftableBigItemStack craftable = new CraftableBigItemStack(result.copy(), recipe);
        craftable.count = 0; // "available" count; the request screen recomputes the craftable amount
        return craftable;
    }

    private static String safePath(ResourceLocation key) {
        return key == null ? "unknown" : (key.getNamespace() + "_" + key.getPath());
    }
}
