/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.client.gui.GuiArchitectTable;
import buildcraft.builders.client.gui.GuiBuilder;
import buildcraft.builders.client.gui.GuiFiller;
import buildcraft.builders.client.render.RenderArchitectTable;
import buildcraft.builders.client.render.RenderBuilder;
import buildcraft.builders.client.render.RenderFiller;
import buildcraft.builders.client.render.RenderQuarry;
import buildcraft.builders.item.ItemSnapshot;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCBuildersClient {
    private BCBuildersClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCBuildersBlocks.QUARRY_TILE.get(), RenderQuarry::new);
        event.registerBlockEntityRenderer(BCBuildersBlocks.FILLER_TILE.get(), RenderFiller::new);
        event.registerBlockEntityRenderer(BCBuildersBlocks.ARCHITECT_TILE.get(), RenderArchitectTable::new);
        event.registerBlockEntityRenderer(BCBuildersBlocks.BUILDER_TILE.get(), RenderBuilder::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCBuildersMenus.FILLER.get(), GuiFiller::new);
            MenuScreens.register(BCBuildersMenus.ARCHITECT_TABLE.get(), GuiArchitectTable::new);
            MenuScreens.register(BCBuildersMenus.BUILDER.get(), GuiBuilder::new);
            // Used snapshots (holding a scan) look different
            ResourceLocation used = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "used");
            for (Item item : new Item[] { BCBuildersBlocks.TEMPLATE.get(), BCBuildersBlocks.BLUEPRINT.get() }) {
                ItemProperties.register(item, used,
                    (stack, level, entity, seed) -> ItemSnapshot.getHeader(stack) == null ? 0 : 1);
            }
        });
    }
}
