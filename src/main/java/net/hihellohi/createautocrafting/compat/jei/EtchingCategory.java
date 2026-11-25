package net.hihellohi.createautocrafting.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.hihellohi.createautocrafting.etching.EtchingRecipe;
import net.hihellohi.createautocrafting.etching.ModRecipes;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI page for the Etching Factory: plate + etching acid -> etched plate. */
public class EtchingCategory implements IRecipeCategory<RecipeHolder<EtchingRecipe>> {
    public static final RecipeType<RecipeHolder<EtchingRecipe>> TYPE =
            RecipeType.createFromVanilla(ModRecipes.ETCHING_TYPE.get());

    private final IDrawable icon;
    private final IDrawableStatic plus;
    private final IDrawableStatic arrow;

    public EtchingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(ModItems.ETCHING_FACTORY.get());
        this.plus = gui.getRecipePlusSign();
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<EtchingRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("createautocrafting.jei.etching");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EtchingRecipe> holder, IFocusGroup focuses) {
        EtchingRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 10).addIngredients(recipe.base());
        var fluidSlot = builder.addSlot(RecipeIngredientRole.INPUT, 38, 10);
        for (var fluid : recipe.fluid().getFluids()) {
            fluidSlot.addFluidStack(fluid.getFluid(), fluid.getAmount());
        }
        fluidSlot.setFluidRenderer(recipe.fluid().amount(), false, 16, 16);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 10).addItemStack(recipe.result());
    }

    @Override
    public void draw(RecipeHolder<EtchingRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        plus.draw(graphics, 24, 11);
        arrow.draw(graphics, 66, 10);
    }
}
