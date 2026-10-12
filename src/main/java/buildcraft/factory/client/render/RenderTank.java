/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.factory.tile.TileTank;

/** Draws the fluid inside a tank. Fluids in a column of tanks are drawn as one, without faces between the tanks. */
public class RenderTank implements BlockEntityRenderer<TileTank> {
    // In pixels: just inside the tank's glass
    private static final float MIN_XZ = 0.13f * 16, MAX_XZ = 0.86f * 16;
    private static final float MIN_Y = 0.01f * 16, MAX_Y = 0.99f * 16;

    public RenderTank(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileTank tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        FluidStack fluid = tile.getFluidForRender(partialTick);
        if (fluid.isEmpty()) {
            return;
        }
        boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
        boolean connectedUp = isFullyConnected(tile, Direction.UP, fluid, partialTick);
        boolean connectedDown = isFullyConnected(tile, Direction.DOWN, fluid, partialTick);

        float bottom = connectedDown ? 0 : MIN_Y;
        float top = connectedUp ? 16 : MAX_Y;
        float height = (top - bottom) * Math.min(1, fluid.getAmount() / (float) tile.tank.getCapacity());
        float y0 = gas ? top - height : bottom;
        float y1 = gas ? top : bottom + height;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        int fluidLight = fluid.getFluid().getFluidType().getLightLevel(fluid);
        int blockLight = Math.max(LightTexture.block(light), fluidLight);
        BoxRenderer box = new BoxRenderer(
            buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack,
            LightTexture.pack(blockLight, LightTexture.sky(light)), OverlayTexture.NO_OVERLAY,
            extensions.getTintColor(fluid)
        );

        for (Direction face : Direction.values()) {
            if ((face == Direction.UP && connectedUp && (gas || height >= top - bottom))
                || (face == Direction.DOWN && connectedDown)) {
                continue;
            }
            switch (face.getAxis()) {
                case X -> box.face(face, MIN_XZ, y0, MIN_XZ, MAX_XZ, y1, MAX_XZ, sprite, MIN_XZ, 16 - y1, MAX_XZ,
                    16 - y0);
                case Z -> box.face(face, MIN_XZ, y0, MIN_XZ, MAX_XZ, y1, MAX_XZ, sprite, MIN_XZ, 16 - y1, MAX_XZ,
                    16 - y0);
                case Y -> box.face(face, MIN_XZ, y0, MIN_XZ, MAX_XZ, y1, MAX_XZ, sprite, MIN_XZ, MIN_XZ, MAX_XZ,
                    MAX_XZ);
            }
        }
    }

    /** @return True if the fluid continues into the tank on the given side, so the face between them is hidden. */
    private static boolean isFullyConnected(TileTank tile, Direction face, FluidStack fluid, float partialTick) {
        if (!(tile.getNeighbourTile(face) instanceof TileTank other) || !TileTank.canTanksConnect(tile, other, face)) {
            return false;
        }
        FluidStack otherFluid = other.getFluidForRender(partialTick);
        if (otherFluid.isEmpty() || !otherFluid.isFluidEqual(fluid)) {
            return false;
        }
        if (fluid.getFluid().getFluidType().isLighterThanAir()) {
            face = face.getOpposite();
        }
        return otherFluid.getAmount() >= other.tank.getCapacity() || face == Direction.UP;
    }
}
