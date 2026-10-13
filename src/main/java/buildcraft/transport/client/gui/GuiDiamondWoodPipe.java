/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.gui;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.api.transport.pipe.IFlowItems;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.transport.container.ContainerDiamondWoodPipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;

/** The filters of a diamond wooden pipe, and the buttons that choose how they are used. */
public class GuiDiamondWoodPipe extends GuiBC<ContainerDiamondWoodPipe> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/pipe_emerald.png");
    private static final ResourceLocation TEXTURE_BUTTON =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/pipe_emerald_button.png");
    private static final int BUTTON_Y = 41, BUTTON_SIZE = 18;

    public GuiDiamondWoodPipe(ContainerDiamondWoodPipe container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 175, 161);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    private PipeBehaviourWoodDiamond behaviour() {
        return menu.behaviour;
    }

    /** Fluid pipes can't extract one filter at a time. */
    private int getModeCount() {
        return behaviour().pipe.getFlow() instanceof IFlowItems ? 3 : 2;
    }

    private int getButtonX(int index) {
        return 7 + index * BUTTON_SIZE;
    }

    private boolean isOnButton(double mouseX, double mouseY, int index) {
        double x = mouseX - leftPos - getButtonX(index);
        double y = mouseY - topPos - BUTTON_Y;
        return x >= 0 && y >= 0 && x < BUTTON_SIZE && y < BUTTON_SIZE;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        for (int i = 0; i < getModeCount(); i++) {
            boolean active = behaviour().filterMode.ordinal() == i;
            boolean hovered = isOnButton(mouseX, mouseY, i);
            // The background states are: disabled, normal, hovered, active, active and hovered
            int state = active ? (hovered ? 4 : 3) : (hovered ? 2 : 1);
            int x = leftPos + getButtonX(i);
            int y = topPos + BUTTON_Y;
            graphics.blit(TEXTURE_BUTTON, x, y, state * BUTTON_SIZE, 0, BUTTON_SIZE, BUTTON_SIZE);
            graphics.blit(TEXTURE_BUTTON, x + 1, y + 1, 19 + i * 18, 19, 16, 16);
        }
        if (behaviour().filterMode == FilterMode.ROUND_ROBIN) {
            // Shows the filter that is used next
            boolean valid = behaviour().filterValid;
            int x = valid ? 18 * behaviour().currentFilter : 0;
            graphics.blit(TEXTURE, leftPos + 6 + x, topPos + 16, 176, valid ? 0 : 20, 20, 20);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < getModeCount(); i++) {
            if (isOnButton(mouseX, mouseY, i)) {
                String mode = FilterMode.VALUES[i].name().toLowerCase(Locale.ROOT);
                graphics.renderTooltip(font, Component.translatable("buildcraft.gui.diamond_wood_pipe." + mode),
                    mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < getModeCount(); i++) {
            if (isOnButton(mouseX, mouseY, i)) {
                menu.setFilterMode(FilterMode.VALUES[i]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
