/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.LaserBeamRenderer;
import buildcraft.lib.misc.data.Box;

import buildcraft.builders.filler.FillerBuilder;
import buildcraft.builders.tile.TileFiller;

/** Draws the box of the filler (striped, like in 1.12), lasers to the blocks it is breaking, and the blocks that it is
 * placing flying to where they go. */
public class RenderFiller implements BlockEntityRenderer<TileFiller> {
    public RenderFiller(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileFiller tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        Box box = tile.getBox();
        if (box != null) {
            LaserBeamRenderer.renderBoxEdges(poseStack, buffers, box, origin, LaserBeamRenderer.STRIPES_WRITE);
        }
        FillerBuilder builder = tile.getBuilder();
        Vec3 top = new Vec3(0.5, 1, 0.5);
        for (FillerBuilder.Task task : builder.breakTasks) {
            Vec3 target = Vec3.atCenterOf(task.pos).subtract(origin);
            int beam = (int) Math.min(3, task.getProgress() * 4);
            LaserBeamRenderer.render(poseStack, buffers, top, target, LaserBeamRenderer.POWER_BEAMS[beam]);
        }
        for (FillerBuilder.Task task : builder.placeTasks) {
            // The block flies in an arc from the filler to where it goes
            Vec3 height = Vec3.atLowerCornerOf(task.pos.subtract(tile.getBlockPos()));
            double progress = task.getProgress();
            Vec3 pos = height.scale(progress).add(0, Math.sin(progress * Math.PI) * (Math.abs(height.y) + 1), 0)
                .add(0.5, 1, 0.5);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, pos.z);
            poseStack.scale(0.6f, 0.6f, 0.6f);
            int itemLight = LevelRenderer.getLightColor(tile.getLevel(), BlockPos.containing(pos.add(origin)));
            Minecraft.getInstance().getItemRenderer().renderStatic(task.item, ItemDisplayContext.GROUND, itemLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, tile.getLevel(), 0);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileFiller tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
