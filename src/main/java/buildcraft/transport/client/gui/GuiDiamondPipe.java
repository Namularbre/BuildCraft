/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.transport.container.ContainerDiamondPipe;

/** The filters of a diamond pipe: each row is the colour of the side it sends things to. */
public class GuiDiamondPipe extends GuiBC<ContainerDiamondPipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/filter.png"
    );
    private static final int SIZE_X = 175, SIZE_Y = 225;

    public GuiDiamondPipe(ContainerDiamondPipe container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
        inventoryLabelY = 140 - 11;
    }
}
