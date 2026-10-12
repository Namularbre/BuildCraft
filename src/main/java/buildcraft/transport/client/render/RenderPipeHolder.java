/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.tile.TilePipeHolder;

/** Renders the items travelling through item pipes. */
public class RenderPipeHolder implements BlockEntityRenderer<TilePipeHolder> {
    private static final float ITEM_SCALE = 0.4f;

    private final ItemRenderer itemRenderer;

    public RenderPipeHolder(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TilePipeHolder tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
        int light, int overlay) {
        Pipe pipe = tile.getPipe();
        if (pipe == null || !(pipe.getFlow() instanceof PipeFlowItems flow)) {
            return;
        }
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
}
