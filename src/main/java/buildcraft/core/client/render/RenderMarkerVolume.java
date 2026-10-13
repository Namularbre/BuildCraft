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
            LaserBeamRenderer.renderBoxEdges(poseStack, buffers, box, origin, CONNECTED);
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

    @Override
    public boolean shouldRenderOffScreen(TileMarkerVolume tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
