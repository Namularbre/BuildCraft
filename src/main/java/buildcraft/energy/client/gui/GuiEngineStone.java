/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.energy.container.ContainerEngineStone;

public class GuiEngineStone extends GuiBC<ContainerEngineStone> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/stirling_engine.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 166;
    /** Where the flame is drawn, and where it is in the texture. */
    private static final int FLAME_X = 81, FLAME_Y = 25, FLAME_SIZE = 14, FLAME_U = 176;

    public GuiEngineStone(ContainerEngineStone container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        double amount = menu.getFuelLeft();
        if (amount > 0) {
            int flameHeight = (int) Math.ceil(amount * FLAME_SIZE);
            graphics.blit(
                TEXTURE, leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - flameHeight, FLAME_U, FLAME_SIZE
                    - flameHeight, FLAME_SIZE, flameHeight
            );
        }
    }
}
