/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

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
        BoxRenderer box = new BoxRenderer(buffer, poseStack, light, overlay);

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
}
