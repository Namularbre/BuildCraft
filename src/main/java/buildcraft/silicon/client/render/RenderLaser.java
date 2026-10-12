/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.client.render.LaserBeamRenderer;

import buildcraft.BuildCraft;
import buildcraft.silicon.tile.TileLaser;

/** Draws the beam of a laser to the table it powers: red, yellow, green or blue depending on the power it sends. */
public class RenderLaser implements BlockEntityRenderer<TileLaser> {
    /** The beam is only drawn above this average power. */
    private static final long MIN_POWER = MjAPI.MJ / 5;

    public RenderLaser(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileLaser tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        Vec3 target = tile.getLaserPos();
        long avg = tile.getAverageClient();
        if (target == null || avg <= MIN_POWER) {
            return;
        }
        ResourceLocation[] beams = LaserBeamRenderer.POWER_BEAMS;
        long scaled = (avg + MIN_POWER) * (beams.length - 1) / TileLaser.MAX_POWER_PER_TICK;
        int index = (int) Math.min(beams.length - 1, scaled);
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        LaserBeamRenderer.render(poseStack, buffers, tile.getEmitterPos().subtract(origin), target.subtract(origin),
            beams[index]);
    }

    @Override
    public boolean shouldRenderOffScreen(TileLaser tile) {
        return true;
    }
}
