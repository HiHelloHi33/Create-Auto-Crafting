package net.hihellohi.createautocrafting.etching;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Circuit Assembler job: one {@code plate} plus one of each listed component become {@code result}
 * (which carries the count, so a better plate can give more circuits). The assembler runs the matching
 * recipe that uses the most components, which is how a circuit's tier follows from what you load.
 */
public record AssemblyRecipe(Ingredient plate, List<Ingredient> components, ItemStack result, int work)
        implements Recipe<AssemblyRecipeInput> {

    public static final int DEFAULT_WORK = 160;

    /** Slot indices (1..5) holding each component, in recipe order, or null if something is missing. */
    public int[] matchSlots(AssemblyRecipeInput input) {
        if (input.size() == 0 || !plate.test(input.getItem(0))) {
            return null;
        }
        int[] slots = new int[components.size()];
        boolean[] used = new boolean[input.size()];
        for (int c = 0; c < slots.length; c++) {
            slots[c] = -1;
            for (int s = 1; s < input.size(); s++) {
                if (!used[s] && components.get(c).test(input.getItem(s))) {
                    used[s] = true;
                    slots[c] = s;
                    break;
                }
            }
            if (slots[c] < 0) {
                return null;
            }
        }
        return slots;
    }

    @Override
    public boolean matches(AssemblyRecipeInput input, Level level) {
        return matchSlots(input) != null;
    }

    /** Of all assembly recipes that match, the one using the most components (then the biggest yield). */
    public static RecipeHolder<AssemblyRecipe> findBest(Level level, AssemblyRecipeInput input,
                                                        net.minecraft.world.item.Item wanted) {
        RecipeHolder<AssemblyRecipe> best = null;
        for (RecipeHolder<AssemblyRecipe> holder : level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get())) {
            AssemblyRecipe r = holder.value();
            if (!r.result.is(wanted) || r.matchSlots(input) == null) {
                continue;
            }
            if (best == null
                    || r.components.size() > best.value().components.size()
                    || (r.components.size() == best.value().components.size()
                    && r.result.getCount() > best.value().result.getCount())) {
                best = holder;
            }
        }
        return best;
    }

    @Override
    public ItemStack assemble(AssemblyRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(plate);
        list.addAll(components);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ASSEMBLY_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<AssemblyRecipe> {
        private static final MapCodec<AssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("plate").forGetter(AssemblyRecipe::plate),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(AssemblyRecipe::components),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AssemblyRecipe::result),
                Codec.INT.optionalFieldOf("work", DEFAULT_WORK).forGetter(AssemblyRecipe::work)
        ).apply(i, AssemblyRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AssemblyRecipe::plate,
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), AssemblyRecipe::components,
                ItemStack.STREAM_CODEC, AssemblyRecipe::result,
                ByteBufCodecs.VAR_INT, AssemblyRecipe::work,
                AssemblyRecipe::new);

        @Override
        public MapCodec<AssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
