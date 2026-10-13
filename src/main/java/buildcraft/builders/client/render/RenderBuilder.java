/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.LaserBeamRenderer;
import buildcraft.lib.misc.data.Box;

import buildcraft.builders.tile.TileBuilder;

/** Draws the box that the builder builds (striped), and what it is doing. */
public class RenderBuilder implements BlockEntityRenderer<TileBuilder> {
    public RenderBuilder(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileBuilder tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        Box box = tile.getBox();
        if (box != null) {
            LaserBeamRenderer.renderBoxEdges(poseStack, buffers, box, Vec3.atLowerCornerOf(tile.getBlockPos()),
                LaserBeamRenderer.STRIPES_WRITE);
        }
        RenderSnapshotBuilder.render(tile.getBuilder(), tile.getLevel(), tile.getBlockPos(), poseStack, buffers);
    }

    @Override
    public boolean shouldRenderOffScreen(TileBuilder tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
