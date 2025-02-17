/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.compat.patchouli.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.config.TFCConfig;
import vazkii.patchouli.api.IComponentRenderContext;

public class HeatingComponent extends InputOutputComponent<HeatingRecipe>
{
    @Override
    public void render(PoseStack poseStack, IComponentRenderContext context, float partialTicks, int mouseX, int mouseY)
    {
        if (recipe == null) return;

        renderSetup(poseStack);

        final int v = !recipe.getDisplayOutputFluid().isEmpty() ? 116 : 90;
        GuiComponent.blit(poseStack, 9, 0, 0, v, 98, 26, 256, 256);

        context.renderIngredient(poseStack, 14, 5, mouseX, mouseY, getIngredient(recipe));
        context.renderItemStack(poseStack, 86, 5, mouseX, mouseY, getOutput(recipe));
        renderFluidStack(poseStack, recipe.getDisplayOutputFluid(), 64, 5);

        final MutableComponent tooltip = TFCConfig.CLIENT.heatTooltipStyle.get().format(recipe.getTemperature());
        if (tooltip != null)
        {
            final Font font = Minecraft.getInstance().font;
            final int centerX = 64 - 8 - font.width(tooltip.getString()) / 2; // Page width = 64, Offset = 8,
            font.draw(poseStack, tooltip, centerX, 28, 0x404040);
        }

        poseStack.popPose();
    }

    @Override
    public Ingredient getIngredient(HeatingRecipe recipe)
    {
        return recipe.getIngredient();
    }

    @Override
    public ItemStack getOutput(HeatingRecipe recipe)
    {
        return recipe.getResultItem();
    }

    @Override
    protected RecipeType<HeatingRecipe> getRecipeType()
    {
        return TFCRecipeTypes.HEATING.get();
    }
}
