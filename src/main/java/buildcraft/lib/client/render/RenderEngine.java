/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import buildcraft.lib.engine.TileEngineBase;

/** Renders the moving parts of an engine: the piston plate and the bellows (chamber) below it. The static base and
 * trunk are part of the block model. */
public class RenderEngine<T extends TileEngineBase> implements BlockEntityRenderer<T> {
    private static final float PX = 1 / 16f;

    private final ResourceLocation backTexture;
    private final ResourceLocation sideTexture;
    private final ResourceLocation chamberTexture;

    public RenderEngine(BlockEntityRendererProvider.Context context, ResourceLocation backTexture,
        ResourceLocation sideTexture, ResourceLocation chamberTexture) {
        this.backTexture = backTexture;
        this.sideTexture = sideTexture;
        this.chamberTexture = chamberTexture;
    }

    @Override
    public void render(T engine, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        float progress = engine.getProgressClient(partialTick);
        // How far the plate has moved up, in pixels: up for the first half of the cycle, back down for the second
        float size = progress > 0.5f ? (1 - progress) * (8 * 2 - 0.01f) : progress * (8 * 2 - 0.01f);

        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite back = atlas.apply(backTexture);
        TextureAtlasSprite side = atlas.apply(sideTexture);
        TextureAtlasSprite chamber = atlas.apply(chamberTexture);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(engine.getCurrentFacing().getRotation());
        poseStack.translate(-0.5, -0.5, -0.5);

        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Box box = new Box(buffer, poseStack.last().pose(), poseStack.last().normal(), light, overlay);

        // The plate
        float y0 = 4 + size;
        float y1 = 8 + size;
        box.face(Direction.DOWN, 0, y0, 0, 16, y1, 16, back, 0, 0, 16, 16);
        box.face(Direction.UP, 0, y0, 0, 16, y1, 16, back, 0, 0, 16, 16);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            box.face(dir, 0, y0, 0, 16, y1, 16, side, 0, 0, 16, 4);
        }

        // The bellows
        if (size > 0) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                box.face(dir, 3, 4, 3, 13, 4 + size, 13, chamber, 3, size, 13, 0);
            }
        }

        poseStack.popPose();
    }

    /** Emits textured box faces. Positions and UVs are in pixels (0-16). */
    private record Box(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, int light, int overlay) {
        /** @param v0 The v coordinate at the top edge of the face (for side faces) or at the north edge (for up/down).
         * @param v1 The v coordinate at the opposite edge. */
        void face(Direction dir, float x0, float y0, float z0, float x1, float y1, float z1,
            TextureAtlasSprite sprite, float u0, float v0, float u1, float v1) {
            x0 *= PX;
            y0 *= PX;
            z0 *= PX;
            x1 *= PX;
            y1 *= PX;
            z1 *= PX;
            float su0 = sprite.getU(u0);
            float su1 = sprite.getU(u1);
            float sv0 = sprite.getV(v0);
            float sv1 = sprite.getV(v1);
            // Same corner order as vanilla's FaceInfo, so each face is wound counter-clockwise from outside
            float[][] corners = switch (dir) {
                case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
                case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
                case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
                case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
                case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
                case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
            };
            // The down face starts at the south edge, every other face at the top (or north) edge
            float vStart = dir == Direction.DOWN ? sv1 : sv0;
            float vEnd = dir == Direction.DOWN ? sv0 : sv1;
            vertex(dir, corners[0], su0, vStart);
            vertex(dir, corners[1], su0, vEnd);
            vertex(dir, corners[2], su1, vEnd);
            vertex(dir, corners[3], su1, vStart);
        }

        private void vertex(Direction dir, float[] pos, float u, float v) {
            buffer.vertex(pose, pos[0], pos[1], pos[2])
                .color(1f, 1f, 1f, 1f)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normal, dir.getStepX(), dir.getStepY(), dir.getStepZ())
                .endVertex();
        }
    }
}
