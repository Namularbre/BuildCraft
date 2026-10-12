/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;

/** The blueprint, what it makes, the materials and the results, and the progress of the current craft. */
public class GuiAdvancedCraftingTable extends GuiBC<ContainerAdvancedCraftingTable> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/advanced_crafting_table.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 241;
    private static final int PROGRESS_X = 164, PROGRESS_Y = 7, PROGRESS_WIDTH = 4, PROGRESS_HEIGHT = 70;
    private static final int RESULT_X = 127, RESULT_Y = 33;

    public GuiAdvancedCraftingTable(ContainerAdvancedCraftingTable container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
        inventoryLabelY = 153 - 11;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        TileAdvancedCraftingTable tile = menu.tile;
        double progress = Math.min(1, (double) tile.power / TileAdvancedCraftingTable.POWER_REQ);
        int height = (int) Math.ceil(PROGRESS_HEIGHT * progress);
        if (height > 0) {
            int offset = PROGRESS_HEIGHT - height;
            graphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y + offset, SIZE_X, offset,
                PROGRESS_WIDTH, height);
        }
        ItemStack result = tile.getResultClient();
        if (!result.isEmpty()) {
            graphics.renderItem(result, leftPos + RESULT_X, topPos + RESULT_Y);
            graphics.renderItemDecorations(font, result, leftPos + RESULT_X, topPos + RESULT_Y);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        ItemStack result = menu.tile.getResultClient();
        if (!result.isEmpty() && mouseX >= leftPos + RESULT_X && mouseX < leftPos + RESULT_X + 16
            && mouseY >= topPos + RESULT_Y && mouseY < topPos + RESULT_Y + 16) {
            graphics.renderTooltip(font, result, mouseX, mouseY);
        } else if (mouseX >= leftPos + PROGRESS_X && mouseX < leftPos + PROGRESS_X + PROGRESS_WIDTH
            && mouseY >= topPos + PROGRESS_Y && mouseY < topPos + PROGRESS_Y + PROGRESS_HEIGHT) {
            TileAdvancedCraftingTable tile = menu.tile;
            graphics.renderComponentTooltip(font, List.of(
                Component.translatable("buildcraft.gui.laser_table.required",
                    MjAPI.formatMj(TileAdvancedCraftingTable.POWER_REQ)),
                Component.translatable("buildcraft.gui.laser_table.stored", MjAPI.formatMj(tile.power)),
                Component.translatable("buildcraft.gui.laser_table.rate", MjAPI.formatMj(tile.avgPowerClient))
            ), mouseX, mouseY);
        }
    }
}
