/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;

/** Draws laser beams: thin, glowing, textured bars between two points. */
public final class LaserBeamRenderer {
    /** The beam textures, from the weakest (red) to the strongest (blue). */
    public static final ResourceLocation[] POWER_BEAMS = {
        beam("low"), beam("med"), beam("high"), beam("full")
    };
    /** The beam is 1 pixel thick. */
    private static final float HALF_WIDTH = 0.5f;

    private LaserBeamRenderer() {}

    /** The striped beam that outlines an area that is about to be worked on (like the area of a quarry). */
    public static final ResourceLocation STRIPES_WRITE = beam("stripes_write");
    /** The striped beam that outlines an area that is being read (like the area that an architect table scans). */
    public static final ResourceLocation STRIPES_READ = beam("stripes_read");

    private static ResourceLocation beam(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/laser_beam/" + name);
    }

    /** @param from Where the beam starts, relative to the origin of the pose.
     * @param to Where the beam ends, relative to the origin of the pose. */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to,
        ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        Vec3 beam = to.subtract(from);
        float length = (float) beam.length() * 16;
        if (length < 0.01f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        // The beam is drawn along +Z, then turned to point at the target
        Vector3f direction = new Vector3f((float) beam.x, (float) beam.y, (float) beam.z).normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), direction));
        BoxRenderer box = new BoxRenderer(buffers.getBuffer(Sheets.cutoutBlockSheet()), poseStack,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        // One block at a time, so that the texture repeats
        for (float z0 = 0; z0 < length; z0 += 16) {
            float z1 = Math.min(length, z0 + 16);
            for (Direction face : Direction.values()) {
                switch (face.getAxis()) {
                    case X -> box.face(face, -HALF_WIDTH, -HALF_WIDTH, z0, HALF_WIDTH, HALF_WIDTH, z1, sprite, 0, 0,
                        z1 - z0, 1);
                    // Turned, so that the texture goes along the beam like on the other faces
                    case Y -> box.faceRotated(face, -HALF_WIDTH, -HALF_WIDTH, z0, HALF_WIDTH, HALF_WIDTH, z1, sprite, 0,
                        0, z1 - z0, 1);
                    case Z -> {
                        if ((face == Direction.NORTH && z0 == 0) || (face == Direction.SOUTH && z1 == length)) {
                            box.face(face, -HALF_WIDTH, -HALF_WIDTH, z0, HALF_WIDTH, HALF_WIDTH, z1, sprite, 0, 0, 1,
                                1);
                        }
                    }
                }
            }
        }
        poseStack.popPose();
    }

    /** Draws the 12 edges of the box, through the middle of the blocks on its edges.
     *
     * @param origin The position of the origin of the pose, in the level. */
    public static void renderBoxEdges(PoseStack poseStack, MultiBufferSource buffers, Box box, Vec3 origin,
        ResourceLocation texture) {
        Vec3 min = Vec3.atCenterOf(box.min()).subtract(origin);
        Vec3 max = Vec3.atCenterOf(box.max()).subtract(origin);
        double[] xs = { min.x, max.x }, ys = { min.y, max.y }, zs = { min.z, max.z };
        for (double y : ys) {
            for (double z : zs) {
                edge(poseStack, buffers, new Vec3(min.x, y, z), new Vec3(max.x, y, z), texture);
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                edge(poseStack, buffers, new Vec3(x, min.y, z), new Vec3(x, max.y, z), texture);
            }
            for (double y : ys) {
                edge(poseStack, buffers, new Vec3(x, y, min.z), new Vec3(x, y, max.z), texture);
            }
        }
    }

    private static void edge(PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to,
        ResourceLocation texture) {
        if (from.distanceToSqr(to) > 1e-6) {
            render(poseStack, buffers, from, to, texture);
        }
    }
}
