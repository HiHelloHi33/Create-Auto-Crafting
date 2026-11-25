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
import net.hihellohi.createautocrafting.etching.AssemblyRecipe;
import net.hihellohi.createautocrafting.etching.ModRecipes;
import net.hihellohi.createautocrafting.items.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI page for the Circuit Assembler: a plate in the middle, components around it, circuits out. */
public class AssemblyCategory implements IRecipeCategory<RecipeHolder<AssemblyRecipe>> {
    public static final RecipeType<RecipeHolder<AssemblyRecipe>> TYPE =
            RecipeType.createFromVanilla(ModRecipes.ASSEMBLY_TYPE.get());

    private static final int CX = 46;
    private static final int CY = 44;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public AssemblyCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(ModItems.CIRCUIT_ASSEMBLER.get());
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<AssemblyRecipe>> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("createautocrafting.jei.assembly");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 84;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AssemblyRecipe> holder, IFocusGroup focuses) {
        AssemblyRecipe recipe = holder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, CX - 8, CY - 8).addIngredients(recipe.plate());
        int n = recipe.components().size();
        for (int i = 0; i < n; i++) {
            // Same pentagon as the GUI: the first component at the top, then clockwise.
            double angle = Math.toRadians(-90 + 72.0 * i);
            int x = CX + (int) Math.round(32 * Math.cos(angle));
            int y = CY + (int) Math.round(32 * Math.sin(angle));
            builder.addSlot(RecipeIngredientRole.INPUT, x - 8, y - 8).addIngredients(recipe.components().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 126, CY - 8).addItemStack(recipe.result());
        builder.addSlot(RecipeIngredientRole.CATALYST, 126, CY + 14)
                .addItemStack(new net.minecraft.world.item.ItemStack(ModItems.CIRCUIT_ASSEMBLER.get()));
    }

    @Override
    public void draw(RecipeHolder<AssemblyRecipe> holder, IRecipeSlotsView slots, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, 96, CY - 9);
        Component count = Component.translatable("createautocrafting.jei.assembly.count",
                holder.value().result().getCount());
        graphics.drawString(Minecraft.getInstance().font, count, 148 - Minecraft.getInstance().font.width(count),
                CY - 22, 0xFF555555, false);
    }
}
