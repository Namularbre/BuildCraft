/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client.gui;

import java.util.Optional;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import net.minecraftforge.fluids.capability.templates.FluidTank;

import buildcraft.lib.client.gui.FluidGauge;
import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.energy.container.ContainerEngineIron;

public class GuiEngineIron extends GuiBC<ContainerEngineIron> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "textures/gui/combustion_engine.png"
    );
    private static final int SIZE_X = 176, SIZE_Y = 177;
    private static final int TANK_Y = 18, TANK_WIDTH = 16, TANK_HEIGHT = 60;
    /** The x position of the fuel, coolant and residue tanks. */
    private static final int[] TANK_X = { 26, 80, 134 };
    /** Where the tank's scale (drawn over the fluid) is in the texture. */
    private static final int OVERLAY_U = 176, OVERLAY_V = 0;
    private static final String[] TANK_NAMES = { "fuel", "coolant", "residue" };

    public GuiEngineIron(ContainerEngineIron container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, SIZE_X, SIZE_Y);
    }

    private FluidTank tank(int index) {
        return index == 0 ? menu.tile.tankFuel : index == 1 ? menu.tile.tankCoolant : menu.tile.tankResidue;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        for (int i = 0; i < TANK_X.length; i++) {
            int x = leftPos + TANK_X[i];
            int y = topPos + TANK_Y;
            FluidGauge.draw(graphics, tank(i).getFluid(), tank(i).getCapacity(), x, y, TANK_WIDTH, TANK_HEIGHT);
            graphics.blit(TEXTURE, x, y, OVERLAY_U, OVERLAY_V, TANK_WIDTH, TANK_HEIGHT);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < TANK_X.length; i++) {
            int x = leftPos + TANK_X[i];
            int y = topPos + TANK_Y;
            if (FluidGauge.isMouseOver(mouseX, mouseY, x, y, TANK_WIDTH, TANK_HEIGHT)) {
                Component name = Component.translatable("buildcraft.gui.tank." + TANK_NAMES[i]);
                graphics.renderTooltip(
                    font, FluidGauge.tooltip(name, tank(i).getFluid(), tank(i).getCapacity()), Optional.empty(),
                    mouseX, mouseY
                );
            }
        }
    }
}
