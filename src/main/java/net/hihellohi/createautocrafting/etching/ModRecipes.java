package net.hihellohi.createautocrafting.etching;

import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, CreateAutoCrafting.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateAutoCrafting.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EtchingRecipe>> ETCHING_TYPE =
            TYPES.register("etching", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "etching")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EtchingRecipe>> ETCHING_SERIALIZER =
            SERIALIZERS.register("etching", EtchingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyRecipe>> ASSEMBLY_TYPE =
            TYPES.register("assembly", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyRecipe>> ASSEMBLY_SERIALIZER =
            SERIALIZERS.register("assembly", AssemblyRecipe.Serializer::new);

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }
}
