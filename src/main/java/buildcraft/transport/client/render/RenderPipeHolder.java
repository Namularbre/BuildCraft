/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.EnumWirePart;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.BuildCraft;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.EnumWireBetween;
import buildcraft.transport.wire.WireManager;

/** Renders what moves through pipes (travelling items, fluids, and the flow of power in kinesis pipes), and the
 * wires. */
public class RenderPipeHolder implements BlockEntityRenderer<TilePipeHolder> {
    private static final float ITEM_SCALE = 0.4f;
    private static final ResourceLocation POWER_FLOW = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/pipe/power_flow"
    );
    private static final ResourceLocation COLOUR_ITEM_BOX = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/pipe/colour_item_box"
    );
    /** How far the box around coloured items goes from their centre, in pixels. */
    private static final float COLOUR_BOX_RADIUS = 0.2f * 16;
    /** The colours of the boxes around coloured items (brighter than the dye colours), by {@link DyeColor#getId()}. */
    private static final int[] LIGHT_HEX = {
        0xe4e4e4, 0xEA7835, 0xD943C6, 0x66AAFF, 0xFFD91C, 0x39D52E, 0xD97199, 0x7A7A7A,
        0xa0a7a7, 0x299799, 0x7e34bf, 0x253193, 0x89502D, 0x007F0E, 0xBE2B27, 0x181414
    };

    private final ItemRenderer itemRenderer;

    public RenderPipeHolder(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TilePipeHolder tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
        int light, int overlay) {
        Pipe pipe = tile.getPipe();
        if (pipe == null) {
            return;
        }
        if (!tile.wireManager.parts.isEmpty()) {
            renderWires(tile.wireManager, poseStack, buffers, light);
        }
        if (pipe.getFlow() instanceof PipeFlowItems flow) {
            renderItems(tile, flow, partialTick, poseStack, buffers, light, overlay);
        } else if (pipe.getFlow() instanceof PipeFlowPower flow) {
            renderPower(pipe, flow, partialTick, poseStack, buffers);
        } else if (pipe.getFlow() instanceof PipeFlowFluids flow) {
            renderFluids(pipe, flow, partialTick, poseStack, buffers, light);
        }
    }

    private void renderItems(TilePipeHolder tile, PipeFlowItems flow, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers, int light, int overlay) {
        BlockPos pos = tile.getBlockPos();
        long now = tile.getLevel().getGameTime();
        for (TravellingItem item : flow.getAllItemsForRender()) {
            ItemStack stack = item.getStack();
            if (stack.isEmpty()) {
                continue;
            }
            Vec3 at = item.getRenderPosition(pos, now, partialTick, flow);
            poseStack.pushPose();
            poseStack.translate(at.x - pos.getX(), at.y - pos.getY() - ITEM_SCALE * 0.25, at.z - pos.getZ());
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            poseStack.mulPose(Axis.YP.rotationDegrees((now + partialTick) * 2));
            itemRenderer.renderStatic(
                stack, ItemDisplayContext.GROUND, light, overlay, poseStack, buffers, tile.getLevel(), 0
            );
            poseStack.popPose();
            if (item.colour != null) {
                Vec3 local = at.subtract(pos.getX(), pos.getY(), pos.getZ());
                renderColourBox(item.colour, local, poseStack, buffers, light);
            }
        }
    }

    /** Draws a box of the colour of an item (given by a lens or a gate) around it. */
    private static void renderColourBox(DyeColor colour, Vec3 at, PoseStack poseStack, MultiBufferSource buffers,
        int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(COLOUR_ITEM_BOX);
        BoxRenderer box = new BoxRenderer(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack, light,
            OverlayTexture.NO_OVERLAY, 0xFF_00_00_00 | LIGHT_HEX[colour.getId()]);
        float r = COLOUR_BOX_RADIUS;
        float x = (float) at.x * 16, y = (float) at.y * 16, z = (float) at.z * 16;
        for (Direction face : Direction.values()) {
            box.face(face, x - r, y - r, z - r, x + r, y + r, z + r, sprite, 0, 0, 16, 16);
        }
    }

    /** Draws a glowing bar along each connection, as thick as the power going through it, with a texture that scrolls
     * in the direction the power moves. */
    private static void renderPower(Pipe pipe, PipeFlowPower flow, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers) {
        double centrePower = 0;
        double[] power = new double[6];
        for (Direction side : Direction.values()) {
            int i = side.ordinal();
            power[i] = flow.getSection(side).displayPower / (double) MjAPI.MJ;
            centrePower = Math.max(centrePower, power[i]);
        }
        if (centrePower <= 0) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(POWER_FLOW);
        BoxRenderer box = new BoxRenderer(
            buffers.getBuffer(Sheets.cutoutBlockSheet()), poseStack, LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY
        );

        float centreRadius = (float) Math.min(0.248, 0.248 * centrePower);
        for (Direction side : Direction.values()) {
            if (!pipe.isConnected(side) || power[side.ordinal()] <= 0) {
                continue;
            }
            PipeFlowPower.Section s = flow.getSection(side);
            float offset = (float) Mth.lerp(partialTick, s.clientDisplayFlowLast, s.clientDisplayFlow);
            float radius = (float) Math.min(0.248, 0.248 * power[side.ordinal()]);
            // From the edge of the center bar to the edge of the block
            float[] from = { 0.5f - radius, 0.5f - radius, 0.5f - radius };
            float[] to = { 0.5f + radius, 0.5f + radius, 0.5f + radius };
            int axis = side.getAxis().ordinal();
            if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                from[axis] = 0.5f + centreRadius;
                to[axis] = 1;
            } else {
                from[axis] = 0;
                to[axis] = 0.5f - centreRadius;
            }
            float[] scroll = new float[3];
            scroll[axis] = offset / 2 * side.getAxisDirection().getStep();
            drawBox(box, sprite, from, to, scroll, side.getOpposite(), 0.5f);
        }

        float[] from = { 0.5f - centreRadius, 0.5f - centreRadius, 0.5f - centreRadius };
        float[] to = { 0.5f + centreRadius, 0.5f + centreRadius, 0.5f + centreRadius };
        float[] scroll = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            scroll[axis] = (float) flow.clientDisplayFlowCentre[axis] / 2;
        }
        drawBox(box, sprite, from, to, scroll, null, 0.5f);
    }

    /** Draws the wires: each corner, and what joins them. Powered wires are lit, and use the bottom row of their
     * texture (the top row is the unpowered one). */
    private static void renderWires(WireManager wires, PoseStack poseStack, MultiBufferSource buffers, int light) {
        var buffer = buffers.getBuffer(Sheets.solidBlockSheet());
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        BoxRenderer off = new BoxRenderer(buffer, poseStack, light, OverlayTexture.NO_OVERLAY);
        BoxRenderer on = new BoxRenderer(buffer, poseStack, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        // Slightly bigger than the boxes that are hit, so that they don't flicker where they meet
        final float grow = 0.01f;
        for (Map.Entry<EnumWirePart, DyeColor> entry : wires.parts.entrySet()) {
            EnumWirePart part = entry.getKey();
            boolean powered = wires.isPowered(part);
            TextureAtlasSprite sprite = atlas.apply(getWireTexture(entry.getValue()));
            int index = (part.x.getStep() > 0 ? 4 : 0) + (part.y.getStep() > 0 ? 2 : 0)
                + (part.z.getStep() > 0 ? 1 : 0);
            float v = powered ? 15 : 0;
            var bb = part.boundingBox;
            float x0 = (float) bb.minX * 16 - grow, y0 = (float) bb.minY * 16 - grow, z0 = (float) bb.minZ * 16 - grow;
            float x1 = (float) bb.maxX * 16 + grow, y1 = (float) bb.maxY * 16 + grow, z1 = (float) bb.maxZ * 16 + grow;
            for (Direction face : Direction.values()) {
                (powered ? on : off).face(face, x0, y0, z0, x1, y1, z1, sprite, index, v, index + 1, v + 1);
            }
        }
        for (Map.Entry<EnumWireBetween, DyeColor> entry : wires.betweens.entrySet()) {
            EnumWireBetween between = entry.getKey();
            boolean powered = wires.isPowered(between.parts[0]);
            TextureAtlasSprite sprite = atlas.apply(getWireTexture(entry.getValue()));
            float v = powered ? 15 : 0;
            var bb = between.boundingBox;
            Direction.Axis axis = between.mainAxis;
            float gx = axis == Direction.Axis.X ? 0 : grow;
            float gy = axis == Direction.Axis.Y ? 0 : grow;
            float gz = axis == Direction.Axis.Z ? 0 : grow;
            float x0 = (float) bb.minX * 16 - gx, y0 = (float) bb.minY * 16 - gy, z0 = (float) bb.minZ * 16 - gz;
            float x1 = (float) bb.maxX * 16 + gx, y1 = (float) bb.maxY * 16 + gy, z1 = (float) bb.maxZ * 16 + gz;
            float u0 = (float) bb.min(axis) * 16, u1 = (float) bb.max(axis) * 16;
            BoxRenderer box = powered ? on : off;
            for (Direction face : Direction.values()) {
                if (face.getAxis() == axis) {
                    // The ends are hidden in the corners
                    continue;
                }
                // The texture goes along the wire
                boolean rotated = axis == Direction.Axis.Y
                    || (axis == Direction.Axis.Z && face.getAxis() == Direction.Axis.Y);
                if (rotated) {
                    box.faceRotated(face, x0, y0, z0, x1, y1, z1, sprite, u0, v, u1, v + 1);
                } else {
                    box.face(face, x0, y0, z0, x1, y1, z1, sprite, u0, v, u1, v + 1);
                }
            }
        }
    }

    public static ResourceLocation getWireTexture(DyeColor colour) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/wire/" + colour.getName());
    }

    /** Draws the fluid in each section of the pipe: horizontal sections fill up from the bottom (or from the top, for
     * gases), and vertical ones get thicker as they fill. */
    private static void renderFluids(Pipe pipe, PipeFlowFluids flow, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers, int light) {
        FluidStack fluid = flow.getFluidStackForRender();
        if (fluid.isEmpty()) {
            return;
        }
        double[] amounts = flow.getAmountsForRender(partialTick);
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
        boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
        double capacity = flow.capacity;

        boolean horizontal = false;
        boolean vertical = pipe.isConnected(gas ? Direction.DOWN : Direction.UP);

        for (Direction face : Direction.values()) {
            double amount = amounts[face.get3DDataValue()];
            if (amount <= 0) {
                continue;
            }
            float size = pipe.getConnectedDist(face);
            if (face.getAxis() != Direction.Axis.Y) {
                horizontal |= pipe.isConnected(face);
            }
            float[] centre = { 0.5f, 0.5f, 0.5f };
            float[] radius = { 0.24f, 0.24f, 0.24f };
            int axis = face.getAxis().ordinal();
            centre[axis] += face.getAxisDirection().getStep() * (0.245f + size / 2);
            radius[axis] = 0.005f + size / 2;
            double fill = amount / capacity;
            if (face.getAxis() == Direction.Axis.Y) {
                float perc = (float) Math.sqrt(Math.min(1, fill));
                radius[0] = perc * 0.24f;
                radius[2] = perc * 0.24f;
                fill = 1;
            }
            drawFluidBox(box, sprite, centre, radius, fill, gas);
        }

        double amount = amounts[6];
        if (amount <= 0) {
            return;
        }
        double fill = Math.min(1, amount / capacity);
        float horizPos = 0.26f;
        if (horizontal | !vertical) {
            float[] centre = { 0.5f, 0.5f, 0.5f };
            float[] radius = { 0.24f, 0.24f, 0.24f };
            drawFluidBox(box, sprite, centre, radius, fill, gas);
            horizPos += (float) (0.48 * fill);
        }
        if (vertical && horizPos < 0.74f) {
            float perc = (float) Math.sqrt(fill) * 0.24f;
            float yMin = gas ? 0.26f : horizPos;
            float yMax = gas ? 1 - horizPos : 0.74f;
            float[] centre = { 0.5f, (yMin + yMax) / 2, 0.5f };
            float[] radius = { perc, (yMax - yMin) / 2, perc };
            drawFluidBox(box, sprite, centre, radius, 1, gas);
        }
    }

    /** Draws a box of fluid around centre, filled up to the given fraction of its height. */
    private static void drawFluidBox(BoxRenderer box, TextureAtlasSprite sprite, float[] centre, float[] radius,
        double fill, boolean gas) {
        float[] from = { centre[0] - radius[0], centre[1] - radius[1], centre[2] - radius[2] };
        float[] to = { centre[0] + radius[0], centre[1] + radius[1], centre[2] + radius[2] };
        float height = (float) ((to[1] - from[1]) * Math.max(0, Math.min(1, fill)));
        if (height <= 0) {
            return;
        }
        if (gas) {
            from[1] = to[1] - height;
        } else {
            to[1] = from[1] + height;
        }
        drawBox(box, sprite, from, to, new float[3], null, 1);
    }

    /** Draws a box (in block units), with UVs taken from its position (scaled by uvScale) and moved by scroll
     * pixels. */
    private static void drawBox(BoxRenderer box, TextureAtlasSprite sprite, float[] from, float[] to, float[] scroll,
        Direction skip, float uvScale) {
        float x0 = from[0] * 16, y0 = from[1] * 16, z0 = from[2] * 16;
        float x1 = to[0] * 16, y1 = to[1] * 16, z1 = to[2] * 16;
        float ux0 = x0 * uvScale + scroll[0], uy0 = y0 * uvScale + scroll[1], uz0 = z0 * uvScale + scroll[2];
        float ux1 = x1 * uvScale + scroll[0], uy1 = y1 * uvScale + scroll[1], uz1 = z1 * uvScale + scroll[2];
        for (Direction face : Direction.values()) {
            if (face == skip) {
                continue;
            }
            switch (face.getAxis()) {
                case X -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, uz0, uy0, uz1, uy1);
                case Y -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, ux0, uz0, ux1, uz1);
                case Z -> box.face(face, x0, y0, z0, x1, y1, z1, sprite, ux0, uy0, ux1, uy1);
            }
        }
    }
}
