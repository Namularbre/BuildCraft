/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import org.lwjgl.glfw.GLFW;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.builders.container.ContainerArchitectTable;

/** The GUI of the architect table: a blank snapshot goes in, the scan comes out, with the name typed below. */
public class GuiArchitectTable extends GuiBC<ContainerArchitectTable> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/architect.png");
    private EditBox nameField;

    public GuiArchitectTable(ContainerArchitectTable container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 256, 166);
        inventoryLabelX = 88;
        inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new EditBox(font, leftPos + 90, topPos + 62, 156, 12,
            Component.translatable("buildcraft.gui.architect.name"));
        nameField.setMaxLength(ContainerArchitectTable.MAX_NAME_LENGTH);
        nameField.setValue(menu.tile.name);
        nameField.setResponder(menu::sendName);
        addRenderableWidget(nameField);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int width = menu.tile.clientProgress * 24 / 1000;
        if (width > 0) {
            graphics.blit(TEXTURE, leftPos + 159, topPos + 34, 0, 166, width, 17);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x40_40_40, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x40_40_40, false);
        graphics.drawString(font, Component.translatable("buildcraft.gui.architect.name"), 90, 51, 0x40_40_40, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing in the name field mustn't close the GUI (with the inventory key)
        if (nameField.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return nameField.keyPressed(keyCode, scanCode, modifiers) || nameField.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
