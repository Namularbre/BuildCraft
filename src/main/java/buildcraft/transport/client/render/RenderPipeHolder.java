/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.client.render.BoxRenderer;

import buildcraft.BuildCraft;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.tile.TilePipeHolder;

/** Renders what moves through pipes: travelling items, fluids, and the flow of power in kinesis pipes. */
public class RenderPipeHolder implements BlockEntityRenderer<TilePipeHolder> {
    private static final float ITEM_SCALE = 0.4f;
    private static final ResourceLocation POWER_FLOW = ResourceLocation.fromNamespaceAndPath(
        BuildCraft.MODID, "block/pipe/power_flow"
    );

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
