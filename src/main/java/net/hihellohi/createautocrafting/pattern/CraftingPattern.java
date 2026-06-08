package net.hihellohi.createautocrafting.pattern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a physical recipe a Pattern Provider should emit as items.
 * Stored on Logic Coils / PRVs in a later milestone.
 */
public record CraftingPattern(ItemStack result, List<ItemStack> ingredients, int outputCount) {

    /** The ingredients with identical stacks combined into single entries (summed counts). */
    public List<ItemStack> mergedIngredients() {
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            String key = String.valueOf(ForgeRegistries.ITEMS.getKey(ingredient.getItem()))
                    + (ingredient.getTag() == null ? "" : ingredient.getTag());
            ItemStack existing = merged.get(key);
            if (existing == null) {
                merged.put(key, ingredient.copy());
            } else {
                existing.grow(ingredient.getCount());
            }
        }
        return new ArrayList<>(merged.values());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Result", result.save(new CompoundTag()));
        tag.putInt("Count", outputCount);
        ListTag list = new ListTag();
        for (ItemStack ingredient : ingredients) {
            list.add(ingredient.save(new CompoundTag()));
        }
        tag.put("Ingredients", list);
        return tag;
    }

    public static CraftingPattern load(CompoundTag tag) {
        ItemStack result = ItemStack.of(tag.getCompound("Result"));
        int count = tag.getInt("Count");
        List<ItemStack> ingredients = new ArrayList<>();
        ListTag list = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            ingredients.add(ItemStack.of((CompoundTag) entry));
        }
        return new CraftingPattern(result, ingredients, count);
    }
}
