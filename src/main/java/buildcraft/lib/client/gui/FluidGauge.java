/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.gui;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Draws the contents of a tank in a GUI, filling up from the bottom, with a tooltip. */
public final class FluidGauge {
    private FluidGauge() {}

    /** Draws the fluid in the given rectangle, tiling its still texture. */
    public static void draw(GuiGraphics graphics, FluidStack fluid, int capacity, int x, int y, int width,
        int height) {
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int filled = Math.max(1, Math.min(height, (int) ((long) height * fluid.getAmount() / capacity)));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        RenderSystem.setShaderColor(
            ((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, ((tint >>> 24) & 0xFF)
                / 255f
        );
        RenderSystem.enableBlend();
        int bottom = y + height;
        for (int drawn = 0; drawn < filled; drawn += 16) {
            int tileHeight = Math.min(16, filled - drawn);
            for (int dx = 0; dx < width; dx += 16) {
                int tileWidth = Math.min(16, width - dx);
                // Partial tiles squash the texture slightly, which doesn't show on fluid textures
                graphics.blit(x + dx, bottom - drawn - tileHeight, 0, tileWidth, tileHeight, sprite);
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /** @return The tooltip of a tank: its name, and how much of which fluid it holds. */
    public static List<Component> tooltip(Component tankName, FluidStack fluid, int capacity) {
        Component contents = fluid.isEmpty() ? Component.translatable("buildcraft.gui.tank.empty")
            : fluid.getDisplayName();
        return List.of(
            tankName, contents.copy().withStyle(ChatFormatting.GRAY), Component.translatable(
                "buildcraft.gui.tank.amount", fluid.getAmount(), capacity
            ).withStyle(ChatFormatting.GRAY)
        );
    }

    public static boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
