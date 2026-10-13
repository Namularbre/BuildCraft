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

import buildcraft.builders.tile.TileArchitectTable;

/** Draws the box that the architect table scans (with the "read" stripes), and a laser to what it is scanning. */
public class RenderArchitectTable implements BlockEntityRenderer<TileArchitectTable> {
    public RenderArchitectTable(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileArchitectTable tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
        int light, int overlay) {
        Box box = tile.getBox();
        if (box == null) {
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        LaserBeamRenderer.renderBoxEdges(poseStack, buffers, box, origin, LaserBeamRenderer.STRIPES_READ);
        if (tile.clientScanPos != null) {
            LaserBeamRenderer.render(poseStack, buffers, new Vec3(0.5, 1, 0.5),
                Vec3.atCenterOf(tile.clientScanPos).subtract(origin), LaserBeamRenderer.POWER_BEAMS[3]);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileArchitectTable tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
