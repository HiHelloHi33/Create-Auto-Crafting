package net.hihellohi.createautocrafting.pattern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a physical recipe a Pattern Provider should emit as items.
 */
public record CraftingPattern(ItemStack result, List<ItemStack> ingredients, int outputCount) {

    /** Network codec: result + output count + only the non-empty ingredients (matches the 1.20.1 wire form). */
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingPattern> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.result());
                buf.writeVarInt(p.outputCount());
                List<ItemStack> nonEmpty = p.ingredients().stream().filter(s -> !s.isEmpty()).toList();
                buf.writeVarInt(nonEmpty.size());
                for (ItemStack ingredient : nonEmpty) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ingredient);
                }
            },
            buf -> {
                ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                int outputCount = buf.readVarInt();
                int n = buf.readVarInt();
                List<ItemStack> ingredients = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    ingredients.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return new CraftingPattern(result, ingredients, outputCount);
            });

    /** The ingredients with identical stacks combined into single entries (summed counts). */
    public List<ItemStack> mergedIngredients() {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack existing = null;
            for (ItemStack m : merged) {
                if (ItemStack.isSameItemSameComponents(m, ingredient)) {
                    existing = m;
                    break;
                }
            }
            if (existing == null) {
                merged.add(ingredient.copy());
            } else {
                existing.grow(ingredient.getCount());
            }
        }
        return merged;
    }
}
