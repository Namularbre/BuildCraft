/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import buildcraft.BuildCraft;

import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.client.model.PipeBakedModel;
import buildcraft.transport.client.render.RenderPipeHolder;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCTransportClient {
    private BCTransportClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCTransportBlocks.PIPE_HOLDER_TILE.get(), RenderPipeHolder::new);
    }

    /** Replaces the placeholder model of the pipe holder (from its block state file) with the dynamic pipe model. */
    @SubscribeEvent
    public static void replacePipeModel(ModelEvent.ModifyBakingResult event) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/pipe_holder");
        BCTransportBlocks.PIPE_HOLDER.get().getStateDefinition().getPossibleStates().forEach(state -> {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            event.getModels().put(location, new PipeBakedModel(modelId));
        });
    }
}
