/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Client-side base screen for a {@link ContainerBC}: draws a single background texture, the tooltips, and the
 * standard labels. */
public abstract class GuiBC<C extends ContainerBC> extends AbstractContainerScreen<C> {
    protected final ResourceLocation background;

    public GuiBC(C container, Inventory inventory, Component title, ResourceLocation background, int width,
        int height) {
        super(container, inventory, title);
        this.background = background;
        this.imageWidth = width;
        this.imageHeight = height;
        this.inventoryLabelY = height - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
