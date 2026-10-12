/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import buildcraft.BuildCraft;
import buildcraft.lib.client.render.RenderEngine;

import buildcraft.core.BCCoreBlocks;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCCoreClient {
    private BCCoreClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            BCCoreBlocks.ENGINE_REDSTONE_TILE.get(), context -> new RenderEngine<>(
                context, texture("block/engine/redstone_back"), texture("block/engine/redstone_side"), texture(
                    "block/engine/chamber"
                )
            )
        );
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, path);
    }
}
