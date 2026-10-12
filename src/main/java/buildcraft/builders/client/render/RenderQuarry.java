/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.BoxRenderer;
import buildcraft.lib.client.render.LaserBeamRenderer;
import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.builders.tile.TileQuarry.TaskAddFrame;
import buildcraft.builders.tile.TileQuarry.TaskBreakBlock;

/** Draws the quarry's arms along the top of the frame, and its drill going up and down as it breaks blocks. While it
 * builds the frame, a laser goes from the quarry to where it works. */
public class RenderQuarry implements BlockEntityRenderer<TileQuarry> {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/frame/default"
    );
    private static final ResourceLocation DRILL = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/quarry/drill"
    );
    /** The arms are 8 pixels thick, the drill 4. */
    private static final double ARM = 4 / 16D, DRILL_RADIUS = 2 / 16D;

    public RenderQuarry(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileQuarry tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        Box frameBox = tile.getFrameBox();
        if (frameBox == null) {
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        Vec3 drill = tile.getDrillPos(partialTick);
        if (drill == null) {
            renderBuildLaser(tile, poseStack, buffers, origin);
            return;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite frame = atlas.apply(FRAME);
        TextureAtlasSprite drillSprite = atlas.apply(DRILL);
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Level level = tile.getLevel();
        BlockPos min = frameBox.min();
        BlockPos max = frameBox.max();
        double top = max.getY() + 0.5;
        double cx = drill.x + 0.5;
        double cz = drill.z + 0.5;

        // The arms along the top of the frame, crossing above the drill
        renderBar(level, poseStack, buffer, frame, origin,
            new AABB(min.getX() + 0.25, top - ARM, cz - ARM, max.getX() + 0.75, top + ARM, cz + ARM));
        renderBar(level, poseStack, buffer, frame, origin,
            new AABB(cx - ARM, top - ARM, min.getZ() + 0.25, cx + ARM, top + ARM, max.getZ() + 0.75));

        // The drill goes down as it breaks a block, and back up as the block breaks
        double yOffset = 1 + 4 / 16D;
        if (tile.currentTask instanceof TaskBreakBlock task) {
            double progress = task.getClientProgress(partialTick);
            double value = progress < 0.9 ? 1 - progress / 0.9 : (progress - 0.9) / 0.1;
            yOffset = 0.5 + value * (1 + 4 / 16D - 0.5);
        }
        double drillTop = drill.y + 1 + yOffset;
        renderBar(level, poseStack, buffer, frame, origin,
            new AABB(cx - ARM, drillTop, cz - ARM, cx + ARM, top - ARM, cz + ARM));
        renderBar(level, poseStack, buffer, drillSprite, origin, new AABB(
            cx - DRILL_RADIUS, drill.y + yOffset, cz - DRILL_RADIUS, cx + DRILL_RADIUS, drillTop, cz + DRILL_RADIUS
        ));
    }

    /** While the frame is being built, a laser goes from the quarry to the block it is working on. */
    private static void renderBuildLaser(TileQuarry tile, PoseStack poseStack, MultiBufferSource buffers, Vec3 origin) {
        BlockPos target = null;
        if (tile.currentTask instanceof TaskBreakBlock task && task.power > 0) {
            target = task.breakPos;
        } else if (tile.currentTask instanceof TaskAddFrame task && task.power > 0) {
            target = task.framePos;
        }
        if (target != null) {
            LaserBeamRenderer.render(poseStack, buffers, Vec3.atCenterOf(tile.getBlockPos()).subtract(origin),
                Vec3.atCenterOf(target).subtract(origin), LaserBeamRenderer.POWER_BEAMS[0]);
        }
    }

    /** Draws a box textured like blocks (the texture lines up with the block grid), one block at a time so that each
     * part gets the light of the block it is in. Faces between two parts of the box aren't drawn. */
    private static void renderBar(Level level, PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
        Vec3 origin, AABB bar) {
        int x0 = (int) Math.floor(bar.minX), x1 = (int) Math.ceil(bar.maxX);
        int y0 = (int) Math.floor(bar.minY), y1 = (int) Math.ceil(bar.maxY);
        int z0 = (int) Math.floor(bar.minZ), z1 = (int) Math.ceil(bar.maxZ);
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    AABB cell = bar.intersect(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    if (cell.getXsize() <= 0 || cell.getYsize() <= 0 || cell.getZsize() <= 0) {
                        continue;
                    }
                    poseStack.pushPose();
                    poseStack.translate(x - origin.x, y - origin.y, z - origin.z);
                    int light = LevelRenderer.getLightColor(level, new BlockPos(x, y, z));
                    BoxRenderer box = new BoxRenderer(buffer, poseStack, light, OverlayTexture.NO_OVERLAY);
                    float cx0 = (float) (cell.minX - x) * 16, cx1 = (float) (cell.maxX - x) * 16;
                    float cy0 = (float) (cell.minY - y) * 16, cy1 = (float) (cell.maxY - y) * 16;
                    float cz0 = (float) (cell.minZ - z) * 16, cz1 = (float) (cell.maxZ - z) * 16;
                    for (Direction face : Direction.values()) {
                        if (!isOnBoundary(face, cell, bar)) {
                            continue;
                        }
                        switch (face.getAxis()) {
                            case X -> box.face(face, cx0, cy0, cz0, cx1, cy1, cz1, sprite, cz0, 16 - cy1, cz1,
                                16 - cy0);
                            case Z -> box.face(face, cx0, cy0, cz0, cx1, cy1, cz1, sprite, cx0, 16 - cy1, cx1,
                                16 - cy0);
                            case Y -> box.face(face, cx0, cy0, cz0, cx1, cy1, cz1, sprite, cx0, cz0, cx1, cz1);
                        }
                    }
                    poseStack.popPose();
                }
            }
        }
    }

    /** @return True if the given face of the part of the bar is on the outside of the bar. */
    private static boolean isOnBoundary(Direction face, AABB cell, AABB bar) {
        final double epsilon = 1e-6;
        return switch (face) {
            case DOWN -> Math.abs(cell.minY - bar.minY) < epsilon;
            case UP -> Math.abs(cell.maxY - bar.maxY) < epsilon;
            case NORTH -> Math.abs(cell.minZ - bar.minZ) < epsilon;
            case SOUTH -> Math.abs(cell.maxZ - bar.maxZ) < epsilon;
            case WEST -> Math.abs(cell.minX - bar.minX) < epsilon;
            case EAST -> Math.abs(cell.maxX - bar.maxX) < epsilon;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(TileQuarry tile) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
