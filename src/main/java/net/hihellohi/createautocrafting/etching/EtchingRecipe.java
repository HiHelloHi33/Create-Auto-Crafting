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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * An Etching Factory job (data-driven, so it shows up in JEI and datapacks can change it): one
 * {@code base} plate dipped in {@code fluid} becomes one {@code result}.
 */
public record EtchingRecipe(Ingredient base, SizedFluidIngredient fluid, ItemStack result, int work)
        implements Recipe<EtchingRecipeInput> {

    public static final int DEFAULT_WORK = 200;

    @Override
    public boolean matches(EtchingRecipeInput input, Level level) {
        return base.test(input.base());
    }

    @Override
    public ItemStack assemble(EtchingRecipeInput input, HolderLookup.Provider registries) {
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
        list.add(base);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ETCHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ETCHING_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<EtchingRecipe> {
        private static final MapCodec<EtchingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(EtchingRecipe::base),
                SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(EtchingRecipe::fluid),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(EtchingRecipe::result),
                Codec.INT.optionalFieldOf("work", DEFAULT_WORK).forGetter(EtchingRecipe::work)
        ).apply(i, EtchingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, EtchingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, EtchingRecipe::base,
                SizedFluidIngredient.STREAM_CODEC, EtchingRecipe::fluid,
                ItemStack.STREAM_CODEC, EtchingRecipe::result,
                ByteBufCodecs.VAR_INT, EtchingRecipe::work,
                EtchingRecipe::new);

        @Override
        public MapCodec<EtchingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EtchingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
