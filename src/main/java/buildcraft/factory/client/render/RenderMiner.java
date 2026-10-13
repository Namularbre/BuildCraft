/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import java.util.List;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.BuildCraft;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.tile.TileMiner;

/** Draws the LEDs of a pump or a mining well (power and status), and its tube. */
public class RenderMiner implements BlockEntityRenderer<TileMiner> {
    private static final ResourceLocation LED = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/pump/led"
    );

    private static final int[] COLOUR_POWER = new int[16];
    private static final int COLOUR_STATUS_ON = 0xFF_77_DD_77; // a light green
    private static final int COLOUR_STATUS_OFF = 0xFF_1F_10_1B; // black-ish

    // In pixels: the LEDs are 1 pixel big, just outside of the block
    private static final float OUTSIDE = 0.05f;

    private static final List<Direction> HORIZONTAL = Direction.Plane.HORIZONTAL.stream().toList();

    private static final float TUBE_MIN = 4, TUBE_MAX = 12;

    static {
        for (int i = 0; i < COLOUR_POWER.length; i++) {
            int c = (i * 0x40) / COLOUR_POWER.length;
            int r = (i * 0xE0) / COLOUR_POWER.length + 0x1F;
            COLOUR_POWER[i] = 0xFF << 24 | c << 16 | c << 8 | r;
        }
    }

    private final ResourceLocation tube;
    private final Function<BlockState, List<Direction>> ledSides;
    // In pixels
    private final float ledY, powerOffset, statusOffset;

    private RenderMiner(ResourceLocation tube, Function<BlockState, List<Direction>> ledSides, float ledY,
        float powerOffset, float statusOffset) {
        this.tube = tube;
        this.ledSides = ledSides;
        this.ledY = ledY;
        this.powerOffset = powerOffset;
        this.statusOffset = statusOffset;
    }

    /** Pumps have LEDs near the top of every side. */
    public static RenderMiner pump(BlockEntityRendererProvider.Context context) {
        return new RenderMiner(
            ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/pump/tube"),
            state -> HORIZONTAL, 13.5f, 1.5f, 3.5f
        );
    }

    /** Mining wells have LEDs near the bottom of their front. */
    public static RenderMiner miningWell(BlockEntityRendererProvider.Context context) {
        return new RenderMiner(
            ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/mining_well/tube"),
            state -> state.hasProperty(BlockMiningWell.FACING) ? List.of(state.getValue(BlockMiningWell.FACING))
                : List.of(),
            5.5f, 2.5f, 4.5f
        );
    }

    @Override
    public void render(TileMiner tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
        int overlay) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        BlockPos pos = tile.getBlockPos();

        int powerColour = COLOUR_POWER[(int) (tile.getPercentFilledForRender() * (COLOUR_POWER.length - 1))];
        boolean complete = tile.isComplete();
        int statusColour = complete ? COLOUR_STATUS_OFF : COLOUR_STATUS_ON;
        TextureAtlasSprite led = atlas.apply(LED);
        for (Direction side : ledSides.apply(tile.getBlockState())) {
            // The block itself is opaque (so it has no light): use the light in front of the side
            int sideLight = LevelRenderer.getLightColor(tile.getLevel(), pos.relative(side));
            int statusLight = complete ? sideLight : LightTexture.pack(15, LightTexture.sky(sideLight));
            renderLed(poseStack, buffer, side, powerOffset, sideLight, powerColour, led);
            renderLed(poseStack, buffer, side, statusOffset, statusLight, statusColour, led);
        }

        double length = tile.getLength(partialTick);
        if (length > 0) {
            renderTube(tile, poseStack, buffer, (float) length, atlas.apply(tube));
        }
    }

    /** @param offset How far (in pixels) from the middle of the side the LED is. */
    private void renderLed(PoseStack poseStack, VertexConsumer buffer, Direction side, float offset, int light,
        int colour, TextureAtlasSprite sprite) {
        BoxRenderer box = new BoxRenderer(buffer, poseStack, light, OverlayTexture.NO_OVERLAY, colour);
        Direction along = side.getCounterClockWise();
        float centreX = 8 + side.getStepX() * (8 + OUTSIDE) + along.getStepX() * offset;
        float centreZ = 8 + side.getStepZ() * (8 + OUTSIDE) + along.getStepZ() * offset;
        // Flat along the side, half a pixel each way along it
        float halfX = side.getAxis() == Direction.Axis.X ? 0 : 0.5f;
        float halfZ = side.getAxis() == Direction.Axis.Z ? 0 : 0.5f;
        box.face(side, centreX - halfX, ledY - 0.5f, centreZ - halfZ, centreX + halfX, ledY + 0.5f, centreZ + halfZ,
            sprite, 0, 0, 16, 16);
    }

    private static void renderTube(TileMiner tile, PoseStack poseStack, VertexConsumer buffer, float length,
        TextureAtlasSprite sprite) {
        BlockPos pos = tile.getBlockPos();
        // One block at a time, so that each part gets the light of the block it is in
        for (int i = 0; i < Math.ceil(length); i++) {
            float y1 = -i * 16;
            float y0 = -Math.min(i + 1, length) * 16;
            int light = LevelRenderer.getLightColor(tile.getLevel(), pos.below(i + 1));
            BoxRenderer box = new BoxRenderer(buffer, poseStack, light, OverlayTexture.NO_OVERLAY);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                // The texture is drawn sideways: along the tube, then across its 8 pixels
                box.faceRotated(side, TUBE_MIN, y0, TUBE_MIN, TUBE_MAX, y1, TUBE_MAX, sprite, 0, 0, y1 - y0, 8);
            }
            if (i + 1 >= length) {
                box.face(Direction.DOWN, TUBE_MIN, y0, TUBE_MIN, TUBE_MAX, y1, TUBE_MAX, sprite, 0, 8, 8, 16);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TileMiner tile) {
        // The tube can be far below the pump
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
