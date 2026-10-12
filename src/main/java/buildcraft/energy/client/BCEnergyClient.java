/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import buildcraft.BuildCraft;
import buildcraft.lib.client.render.RenderEngine;

import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.client.gui.GuiEngineStone;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCEnergyClient {
    private BCEnergyClient() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(BCEnergyMenus.ENGINE_STONE.get(), GuiEngineStone::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            BCEnergyBlocks.ENGINE_STONE_TILE.get(), context -> new RenderEngine<>(
                context, texture("block/engine/stone_back"), texture("block/engine/stone_side"), texture(
                    "block/engine/chamber"
                )
            )
        );
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, path);
    }
}
