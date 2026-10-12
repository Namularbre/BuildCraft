/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.container.ContainerAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable.AssemblyInstruction;

/** Shows the items put in the table, the recipes they allow (click one to make it), and the progress. */
public class GuiAssemblyTable extends GuiBC<ContainerAssemblyTable> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/assembly_table.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 220;
    private static final int RECIPES_X = 116, RECIPES_Y = 36, RECIPE_COUNT = 12;
    private static final int PROGRESS_X = 86, PROGRESS_Y = 36, PROGRESS_WIDTH = 4, PROGRESS_HEIGHT = 70;
    /** Where the icons of the saved recipe states are in the texture. */
    private static final int ICON_U = SIZE_X, ICON_SAVED_V = 0, ICON_ENOUGH_V = 16, ICON_ACTIVE_V = 32;
    private static final int PROGRESS_V = 48;

    public GuiAssemblyTable(ContainerAssemblyTable container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
        inventoryLabelY = 123 - 11;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 15;
    }

    private List<Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState>> getRecipes() {
        return new ArrayList<>(menu.tile.recipesStates.entrySet());
    }

    private int getRecipeX(int index) {
        return leftPos + RECIPES_X + (index % 3) * 18;
    }

    private int getRecipeY(int index) {
        return topPos + RECIPES_Y + (index / 3) * 18;
    }

    /** @return The index of the recipe shown under the mouse, or -1. */
    private int getRecipeAt(double mouseX, double mouseY) {
        List<Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState>> recipes = getRecipes();
        for (int i = 0; i < Math.min(RECIPE_COUNT, recipes.size()); i++) {
            int x = getRecipeX(i);
            int y = getRecipeY(i);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        TileAssemblyTable tile = menu.tile;
        long target = tile.getTarget();
        if (target > 0) {
            double progress = Math.min(1, (double) tile.power / target);
            int height = (int) Math.ceil(PROGRESS_HEIGHT * progress);
            int offset = PROGRESS_HEIGHT - height;
            graphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y + offset, ICON_U, PROGRESS_V + offset,
                PROGRESS_WIDTH, height);
        }
        List<Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState>> recipes = getRecipes();
        for (int i = 0; i < Math.min(RECIPE_COUNT, recipes.size()); i++) {
            int x = getRecipeX(i);
            int y = getRecipeY(i);
            int iconV = switch (recipes.get(i).getValue()) {
                case SAVED -> ICON_SAVED_V;
                case SAVED_ENOUGH -> ICON_ENOUGH_V;
                case SAVED_ENOUGH_ACTIVE -> ICON_ACTIVE_V;
                default -> -1;
            };
            if (iconV >= 0) {
                graphics.blit(TEXTURE, x, y, ICON_U, iconV, 16, 16);
            }
            ItemStack output = recipes.get(i).getKey().output();
            graphics.renderItem(output, x, y);
            graphics.renderItemDecorations(font, output, x, y);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        int recipe = getRecipeAt(mouseX, mouseY);
        if (recipe >= 0) {
            graphics.renderTooltip(font, getRecipes().get(recipe).getKey().output(), mouseX, mouseY);
        } else if (mouseX >= leftPos + PROGRESS_X && mouseX < leftPos + PROGRESS_X + PROGRESS_WIDTH
            && mouseY >= topPos + PROGRESS_Y && mouseY < topPos + PROGRESS_Y + PROGRESS_HEIGHT) {
            // The power ledger of 1.12, as a tooltip
            TileAssemblyTable tile = menu.tile;
            graphics.renderComponentTooltip(font, List.of(
                Component.translatable("buildcraft.gui.laser_table.required", MjAPI.formatMj(tile.getTarget())),
                Component.translatable("buildcraft.gui.laser_table.stored", MjAPI.formatMj(tile.power)),
                Component.translatable("buildcraft.gui.laser_table.rate", MjAPI.formatMj(tile.avgPowerClient))
            ), mouseX, mouseY);
        }
    }

    /** Clicking a recipe asks for it to be made, or forgets it. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = button == 0 ? getRecipeAt(mouseX, mouseY) : -1;
        if (index >= 0) {
            Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> recipe = getRecipes().get(index);
            EnumAssemblyRecipeState newState = recipe.getValue() == EnumAssemblyRecipeState.POSSIBLE
                ? EnumAssemblyRecipeState.SAVED : EnumAssemblyRecipeState.POSSIBLE;
            menu.tile.setRecipeState(recipe.getKey(), newState);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
