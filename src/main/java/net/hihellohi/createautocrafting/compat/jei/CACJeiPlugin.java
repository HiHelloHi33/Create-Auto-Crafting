package net.hihellohi.createautocrafting.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.screen.PatternCardScreen;
import net.hihellohi.createautocrafting.etching.ModRecipes;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI plugin for this mod: the Etching Factory recipe page (so the circuit line is visible), and a
 * ghost-ingredient handler so JEI items can be dragged onto the Advanced Crafting Blueprint's slots.
 * Loaded only when JEI is present.
 */
@JeiPlugin
public class CACJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new EtchingCategory(gui), new AssemblyCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(EtchingCategory.TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.ETCHING_TYPE.get()));
            registration.addRecipes(AssemblyCategory.TYPE,
                    level.getRecipeManager().getAllRecipesFor(ModRecipes.ASSEMBLY_TYPE.get()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.ETCHING_FACTORY.get()), EtchingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.CIRCUIT_ASSEMBLER.get()), AssemblyCategory.TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PatternCardScreen.class, new PatternGhostHandler());
    }
}
