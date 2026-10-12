/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client.render;

import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.LaserBeamRenderer;
import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;
import buildcraft.core.tile.TileMarkerVolume;

/** Draws the edges of the box of connected markers (once, from its lowest marker), and the lines along which a
 * powered marker looks for others. */
public class RenderMarkerVolume implements BlockEntityRenderer<TileMarkerVolume> {
    private static final ResourceLocation CONNECTED = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/laser_beam/marker_volume_connected"
    );
    private static final ResourceLocation SIGNAL = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/laser_beam/marker_volume_signal"
    );

    public RenderMarkerVolume(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileMarkerVolume tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
        int light, int overlay) {
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        Box box = tile.getBox();
        List<BlockPos> connection = tile.getConnection();
        if (box != null && tile.getBlockPos().equals(connection.stream().min(BlockPos::compareTo).orElse(null))) {
            renderBoxEdges(poseStack, buffers, box, origin);
        }
        if (tile.isShowingSignals()) {
            Set<Direction.Axis> connected = tile.getConnectedAxes();
            Vec3 center = Vec3.atCenterOf(tile.getBlockPos()).subtract(origin);
            for (Direction face : Direction.values()) {
                if (!connected.contains(face.getAxis())) {
                    Vec3 end = center.add(Vec3.atLowerCornerOf(face.getNormal()).scale(TileMarkerVolume.MAX_DISTANCE));
                    LaserBeamRenderer.render(poseStack, buffers, center, end, SIGNAL);
                }
            }
        }
    }

    /** The 12 edges of the box, through the middle of the markers on its corners. */
    private static void renderBoxEdges(PoseStack poseStack, MultiBufferSource buffers, Box box, Vec3 origin) {
        Vec3 min = Vec3.atCenterOf(box.min()).subtract(origin);
        Vec3 max = Vec3.atCenterOf(box.max()).subtract(origin);
        double[] xs = { min.x, max.x }, ys = { min.y, max.y }, zs = { min.z, max.z };
        for (double y : ys) {
            for (double z : zs) {
                edge(poseStack, buffers, new Vec3(min.x, y, z), new Vec3(max.x, y, z));
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                edge(poseStack, buffers, new Vec3(x, min.y, z), new Vec3(x, max.y, z));
            }
            for (double y : ys) {
                edge(poseStack, buffers, new Vec3(x, y, min.z), new Vec3(x, y, max.z));
            }
        }
    }

    private static void edge(PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 1e-6) {
            LaserBeamRenderer.render(poseStack, buffers, from, to, CONNECTED);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileMarkerVolume tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
