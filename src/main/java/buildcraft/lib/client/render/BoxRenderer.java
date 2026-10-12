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

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/** Emits the faces of textured boxes for block entity renderers. Positions and UVs are in pixels (0-16). */
public final class BoxRenderer {
    private static final float PX = 1 / 16f;

    private final VertexConsumer buffer;
    private final Matrix4f pose;
    private final Matrix3f normal;
    private final int light;
    private final int overlay;

    public BoxRenderer(VertexConsumer buffer, PoseStack poseStack, int light, int overlay) {
        this.buffer = buffer;
        this.pose = poseStack.last().pose();
        this.normal = poseStack.last().normal();
        this.light = light;
        this.overlay = overlay;
    }

    /** Draws one face of the box from (x0, y0, z0) to (x1, y1, z1).
     *
     * @param v0 The v coordinate at the top edge of the face (for side faces) or at the north edge (for up/down).
     * @param v1 The v coordinate at the opposite edge. */
    public void face(Direction dir, float x0, float y0, float z0, float x1, float y1, float z1,
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
