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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.LaserBeamRenderer;

import buildcraft.builders.snapshot.SnapshotBuilder;

/** Draws what a {@link SnapshotBuilder} does: lasers to the blocks being broken, and the blocks being placed flying
 * in an arc from the machine to where they go. */
public final class RenderSnapshotBuilder {
    private RenderSnapshotBuilder() {}

    public static void render(SnapshotBuilder builder, Level level, BlockPos machine, PoseStack poseStack,
        MultiBufferSource buffers) {
        Vec3 origin = Vec3.atLowerCornerOf(machine);
        Vec3 top = new Vec3(0.5, 1, 0.5);
        for (SnapshotBuilder.Task task : builder.breakTasks) {
            Vec3 target = Vec3.atCenterOf(task.pos).subtract(origin);
            int beam = (int) Math.min(3, task.getProgress() * 4);
            LaserBeamRenderer.render(poseStack, buffers, top, target, LaserBeamRenderer.POWER_BEAMS[beam]);
        }
        for (SnapshotBuilder.Task task : builder.placeTasks) {
            if (task.item.isEmpty()) {
                continue;
            }
            Vec3 height = Vec3.atLowerCornerOf(task.pos.subtract(machine));
            double progress = task.getProgress();
            Vec3 pos = height.scale(progress).add(0, Math.sin(progress * Math.PI) * (Math.abs(height.y) + 1), 0)
                .add(0.5, 1, 0.5);
            poseStack.pushPose();
            poseStack.translate(pos.x, pos.y, pos.z);
            poseStack.scale(0.6f, 0.6f, 0.6f);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(pos.add(origin)));
            Minecraft.getInstance().getItemRenderer().renderStatic(task.item, ItemDisplayContext.GROUND, light,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, level, 0);
            poseStack.popPose();
        }
    }
}
