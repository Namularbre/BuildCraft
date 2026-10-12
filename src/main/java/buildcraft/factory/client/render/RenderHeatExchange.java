/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraftforge.fluids.capability.templates.FluidTank;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.tile.TileHeatExchange.EnumProgressState;
import buildcraft.factory.tile.TileHeatExchange.Section;
import buildcraft.factory.tile.TileHeatExchange;

/** Draws the fluids in the tanks of the start and the end of a heat exchanger, and (from the start) the two fluids
 * flowing through it in opposite directions. Everything is drawn as if the exchanger faced west (like its model): the
 * start is then to the north, and the rest of the row to the south. */
public class RenderHeatExchange implements BlockEntityRenderer<TileHeatExchange> {
    /** How much smaller than the glass the fluids are, in pixels. */
    private static final float SHRINK = 0.25f;
    /** Where the fluids flow, in pixels from the start of the row: from the start's tank to the end's tank. */
    private static final float FLOW_START = 2;

    public RenderHeatExchange(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileHeatExchange tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
        int light, int overlay) {
        Section section = tile.getSection();
        if (section == null || !tile.getBlockState().hasProperty(BlockHeatExchange.FACING)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        int rotation = BlockHeatExchange.modelRotation(tile.getBlockState().getValue(BlockHeatExchange.FACING));
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
        poseStack.translate(-0.5, 0, -0.5);

        if (section.isStart) {
            // The fluid to heat comes in from below, and the cooled fluid goes out of the outer side
            renderFluid(section.tankInput, 2, 0, 2, 14, 2, 14, poseStack, buffers, light);
            renderFluid(section.tankOutput, 4, 4, 0, 12, 12, 2, poseStack, buffers, light);
            renderFlow(tile, partialTick, poseStack, buffers, light);
        } else {
            // The heated fluid goes out of the top, and the fluid to cool comes in from the outer side
            renderFluid(section.tankOutput, 2, 14, 2, 14, 16, 14, poseStack, buffers, light);
            renderFluid(section.tankInput, 4, 4, 14, 12, 12, 16, poseStack, buffers, light);
        }
        poseStack.popPose();
    }

    /** Draws the fluid being cooled flowing from the end to the start in the inner pipe, and the fluid being heated
     * flowing from the start to the end around it. */
    private static void renderFlow(TileHeatExchange start, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers, int light) {
        TileHeatExchange end = start.getEnd();
        double progress = start.getProgress(partialTick);
        if (end == null || end.getSection() == null || progress <= 0) {
            return;
        }
        float length = (start.getMiddleCount() + 2) * 16 - 2 * FLOW_START;
        float flowEnd = FLOW_START + length;
        float filled = (float) (length * progress);
        boolean stopping = start.getProgressState() == EnumProgressState.STOPPING;

        // The fluid being cooled comes in at the end: it fills from there, and drains towards the start
        FluidStack hot = end.getSection().tankInput.getFluid();
        float hotFrom = stopping ? FLOW_START : flowEnd - filled;
        renderFlowSegment(hot, 4, 12, hotFrom, hotFrom + filled, poseStack, buffers, light);

        FluidStack cold = start.getSection().tankInput.getFluid();
        float coldFrom = stopping ? flowEnd - filled : FLOW_START;
        renderFlowSegment(cold, 2, 14, coldFrom, coldFrom + filled, poseStack, buffers, light);
    }

    /** Draws a square column of fluid along the row, one block at a time (so that the texture repeats). */
    private static void renderFlowSegment(FluidStack fluid, float min, float max, float z0, float z1,
        PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (fluid.isEmpty()) {
            return;
        }
        for (float blockStart = (float) Math.floor(z0 / 16) * 16; blockStart < z1; blockStart += 16) {
            float from = Math.max(z0, blockStart);
            float to = Math.min(z1, blockStart + 16);
            if (to - from < 0.01f) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0, 0, blockStart / 16);
            renderBox(fluid, min, min, from - blockStart, max, max, to - blockStart, poseStack, buffers, light);
            poseStack.popPose();
        }
    }

    /** Draws the fluid in a tank filling the given box (in pixels). Gases fill it from the top. */
    private static void renderFluid(FluidTank tank, float x0, float y0, float z0, float x1, float y1, float z1,
        PoseStack poseStack, MultiBufferSource buffers, int light) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        x0 += SHRINK;
        y0 += SHRINK;
        z0 += SHRINK;
        x1 -= SHRINK;
        y1 -= SHRINK;
        z1 -= SHRINK;
        float height = (y1 - y0) * Math.min(1, fluid.getAmount() / (float) tank.getCapacity());
        if (fluid.getFluid().getFluidType().isLighterThanAir()) {
            y0 = y1 - height;
        } else {
            y1 = y0 + height;
        }
        renderBox(fluid, x0, y0, z0, x1, y1, z1, poseStack, buffers, light);
    }

    private static void renderBox(FluidStack fluid, float x0, float y0, float z0, float x1, float y1, float z1,
        PoseStack poseStack, MultiBufferSource buffers, int light) {
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
            switch (face.getAxis()) {
                case X -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, z0, 16 - y1, z1, 16 - y0);
                case Z -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, x0, 16 - y1, x1, 16 - y0);
                case Y -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, x0, z0, x1, z1);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileHeatExchange tile) {
        return tile.isStart();
    }
}
