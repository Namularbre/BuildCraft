/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.tile.TileAutoWorkbenchItems;

public class GuiAutoCraftItems extends GuiBC<ContainerAutoCraftItems> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/auto_workbench_item.png"
    );
    private static final ResourceLocation TEXTURE_MISC = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/misc_slots.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 197;
    private static final int PROGRESS_X = 90, PROGRESS_Y = 47, PROGRESS_WIDTH = 23, PROGRESS_HEIGHT = 10;
    private static final int RESULT_X = 93, RESULT_Y = 27;
    /** The overlays drawn on a material slot holding the item of its filter (or nothing), or another item. */
    private static final int OVERLAY_SAME_U = 54, OVERLAY_DIFFERENT_U = 72;

    private final ItemStack[] filters = new ItemStack[9];

    public GuiAutoCraftItems(ContainerAutoCraftItems container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        TileAutoWorkbenchItems tile = menu.tile;

        double progress = Math.min(1, Math.max(0, tile.getProgress(partialTick)));
        int width = (int) Math.round(progress * PROGRESS_WIDTH);
        if (width > 0) {
            graphics.blit(TEXTURE, leftPos + PROGRESS_X, topPos + PROGRESS_Y, SIZE_X, 0, width, PROGRESS_HEIGHT);
        }

        ItemStack result = tile.getResultClient();
        if (!result.isEmpty()) {
            graphics.renderItem(result, leftPos + RESULT_X, topPos + RESULT_Y);
            graphics.renderItemDecorations(font, result, leftPos + RESULT_X, topPos + RESULT_Y);
        }

        // Show which item each material slot is for, like the server works it out
        TileAutoWorkbenchItems.updateFilters(result.isEmpty() ? null : tile.invBlueprint, filters);
        for (int s = 0; s < filters.length; s++) {
            ItemStack filter = filters[s];
            if (filter.isEmpty()) {
                continue;
            }
            Slot slot = menu.materialSlots[s];
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            ItemStack real = slot.getItem();
            if (real.isEmpty()) {
                // The item that goes there, under the filter overlay
                graphics.renderItem(filter, x, y);
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.blit(TEXTURE_MISC, x - 1, y - 1, OVERLAY_SAME_U, 0, 18, 18);
                graphics.pose().popPose();
            } else if (!ItemStack.isSameItemSameTags(real, filter)) {
                // A frame around the wrong item (drawn under it)
                graphics.blit(TEXTURE_MISC, x - 1, y - 1, OVERLAY_DIFFERENT_U, 0, 18, 18);
            }
        }
    }
}
