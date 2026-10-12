/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client;

import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.client.gui.GuiAutoCraftItems;
import buildcraft.factory.client.gui.GuiChute;
import buildcraft.factory.client.render.RenderDistiller;
import buildcraft.factory.client.render.RenderHeatExchange;
import buildcraft.factory.client.render.RenderMiner;
import buildcraft.factory.client.render.RenderTank;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCFactoryClient {
    private BCFactoryClient() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCFactoryMenus.CHUTE.get(), GuiChute::new);
            MenuScreens.register(BCFactoryMenus.AUTO_WORKBENCH_ITEMS.get(), GuiAutoCraftItems::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCFactoryBlocks.TANK_TILE.get(), RenderTank::new);
        event.registerBlockEntityRenderer(BCFactoryBlocks.PUMP_TILE.get(), RenderMiner::pump);
        event.registerBlockEntityRenderer(BCFactoryBlocks.MINING_WELL_TILE.get(), RenderMiner::miningWell);
        event.registerBlockEntityRenderer(BCFactoryBlocks.DISTILLER_TILE.get(), RenderDistiller::new);
        event.registerBlockEntityRenderer(BCFactoryBlocks.HEAT_EXCHANGE_TILE.get(), RenderHeatExchange::new);
    }
}
