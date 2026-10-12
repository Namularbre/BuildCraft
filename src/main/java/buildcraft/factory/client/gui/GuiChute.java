/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.factory.container.ContainerChute;

public class GuiChute extends GuiBC<ContainerChute> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/chute.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 153;

    public GuiChute(ContainerChute container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
    }
}
