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
                    case Y -> box.face(face, -HALF_WIDTH, -HALF_WIDTH, z0, HALF_WIDTH, HALF_WIDTH, z1, sprite, 0, 0, 1,
                        z1 - z0);
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
}
