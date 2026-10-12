/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.BuildCraft;
import buildcraft.silicon.tile.TileLaser;

/** Draws the beam of a laser to the table it powers: red, yellow, green or blue depending on the power it sends. */
public class RenderLaser implements BlockEntityRenderer<TileLaser> {
    private static final ResourceLocation[] BEAMS = {
        beam("low"), beam("med"), beam("high"), beam("full")
    };
    /** The beam is only drawn above this average power. */
    private static final long MIN_POWER = MjAPI.MJ / 5;
    /** The beam is 1 pixel thick. */
    private static final float HALF_WIDTH = 0.5f;

    public RenderLaser(BlockEntityRendererProvider.Context context) {}

    private static ResourceLocation beam(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/laser_beam/" + name);
    }

    @Override
    public void render(TileLaser tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        Vec3 target = tile.getLaserPos();
        long avg = tile.getAverageClient();
        if (target == null || avg <= MIN_POWER) {
            return;
        }
        long scaled = (avg + MIN_POWER) * (BEAMS.length - 1) / TileLaser.MAX_POWER_PER_TICK;
        int index = (int) Math.min(BEAMS.length - 1, scaled);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(BEAMS[index]);

        Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
        Vec3 start = tile.getEmitterPos().subtract(origin);
        Vec3 beam = target.subtract(origin).subtract(start);
        float length = (float) beam.length() * 16;

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
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

    @Override
    public boolean shouldRenderOffScreen(TileLaser tile) {
        return true;
    }
}
