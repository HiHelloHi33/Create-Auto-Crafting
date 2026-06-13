package net.hihellohi.createautocrafting.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.hihellohi.createautocrafting.CreateAutoCrafting;
import net.hihellohi.createautocrafting.client.screen.CraftingBlueprintScreen;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI plugin for this mod. Currently registers a ghost-ingredient handler so JEI items can be
 * dragged onto the Crafting Blueprint's recipe/result slots. Loaded only when JEI is present.
 */
@JeiPlugin
public class CACJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateAutoCrafting.MODID, "jei");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(CraftingBlueprintScreen.class, new BlueprintGhostHandler());
    }
}
