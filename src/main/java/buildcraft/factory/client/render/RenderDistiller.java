/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.BuildCraft;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.tile.TileDistiller;

/** Draws the fluids in the three tanks of a distiller, and its two power blocks moving up and down while it works.
 * Everything is drawn as if the distiller faced west (like its model), then turned. */
public class RenderDistiller implements BlockEntityRenderer<TileDistiller> {
    /** How much smaller than the glass the fluids are, in pixels. */
    private static final float SHRINK = 0.25f;
    /** The power block textures: each sprite holds two 16x8 textures, so these are {sprite, v offset}. */
    private static final String[] POWER_SPRITES = { "a", "a", "b", "b", "c", "c", "d" };
    private static final int[] POWER_V = { 0, 8, 0, 8, 0, 8, 0 };

    public RenderDistiller(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileDistiller tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        BlockState state = tile.getBlockState();
        if (!state.hasProperty(BlockDistiller.FACING)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        int rotation = BlockDistiller.modelRotation(state.getValue(BlockDistiller.FACING));
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
        poseStack.translate(-0.5, 0, -0.5);

        renderPowerBlocks(tile, partialTick, poseStack, buffers, light);
        renderTank(tile.tankIn, 0, 0, 4, 8, 16, 12, poseStack, buffers, light);
        renderTank(tile.tankGasOut, 8, 8, 0, 16, 16, 16, poseStack, buffers, light);
        renderTank(tile.tankLiquidOut, 8, 0, 0, 16, 8, 16, poseStack, buffers, light);

        poseStack.popPose();
    }

    private static void renderPowerBlocks(TileDistiller tile, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers, int light) {
        long power = tile.getPowerAverage() / MjAPI.MJ;
        long powerMax = TileDistiller.MAX_MJ_PER_TICK / MjAPI.MJ;
        int index = power <= 0 ? 0 : (int) Math.max(1, Math.min(6, power * 6 / powerMax));
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
            ResourceLocation.fromNamespaceAndPath(
                BuildCraft.MODID, "block/distiller/power_sprite_" + POWER_SPRITES[index]
            )
        );
        int v = POWER_V[index];
        BoxRenderer box = new BoxRenderer(buffers.getBuffer(Sheets.cutoutBlockSheet()), poseStack, light,
            OverlayTexture.NO_OVERLAY);
        float y1 = (float) tile.getPowerBlockHeight1(partialTick) * 12;
        float y2 = (float) tile.getPowerBlockHeight2(partialTick) * 12;
        powerBlock(box, sprite, v, 0, y1, 12);
        powerBlock(box, sprite, v, 0, y2, 0);
    }

    /** A block of 8x4x4 pixels. The face UVs are those of the 1.12 model, squashed into half of the sprite. */
    private static void powerBlock(BoxRenderer box, TextureAtlasSprite sprite, int v, float x, float y, float z) {
        float x1 = x + 8, y1 = y + 4, z1 = z + 4;
        for (Direction face : Direction.values()) {
            switch (face.getAxis()) {
                case Y -> box.face(face, x, y, z, x1, y1, z1, sprite, 4, v, 12, v + 4);
                case Z -> box.face(face, x, y, z, x1, y1, z1, sprite, 4, v + 4, 12, v + 8);
                case X -> box.face(face, x, y, z, x1, y1, z1, sprite, 0, v + 4, 4, v + 8);
            }
        }
    }

    /** Draws the fluid in a tank filling the given box (in pixels). Gases fill it from the top. */
    private static void renderTank(FluidTank tank, float x0, float y0, float z0, float x1, float y1, float z1,
        PoseStack poseStack, MultiBufferSource buffers, int light) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        x0 += SHRINK;
        y0 += SHRINK;
        z0 += SHRINK;
        x1 -= SHRINK;
        y1 -= SHRINK;
        z1 -= SHRINK;
        float height = (y1 - y0) * Math.min(1, fluid.getAmount() / (float) tank.getCapacity());
        if (fluid.getFluid().getFluidType().isLighterThanAir()) {
            y0 = y1 - height;
        } else {
            y1 = y0 + height;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(extensions.getStillTexture(fluid));
        int fluidLight = fluid.getFluid().getFluidType().getLightLevel(fluid);
        int blockLight = Math.max(LightTexture.block(light), fluidLight);
        BoxRenderer box = new BoxRenderer(
            buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack,
            LightTexture.pack(blockLight, LightTexture.sky(light)), OverlayTexture.NO_OVERLAY,
            extensions.getTintColor(fluid)
        );
        for (Direction face : Direction.values()) {
            switch (face.getAxis()) {
                case X -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, z0, 16 - y1, z1, 16 - y0);
                case Z -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, x0, 16 - y1, x1, 16 - y0);
                case Y -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, x0, z0, x1, z1);
            }
        }
    }
}
