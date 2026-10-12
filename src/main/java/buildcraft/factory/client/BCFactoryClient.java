/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.client.render.RenderDistiller;
import buildcraft.factory.client.render.RenderMiner;
import buildcraft.factory.client.render.RenderTank;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCFactoryClient {
    private BCFactoryClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCFactoryBlocks.TANK_TILE.get(), RenderTank::new);
        event.registerBlockEntityRenderer(BCFactoryBlocks.PUMP_TILE.get(), RenderMiner::pump);
        event.registerBlockEntityRenderer(BCFactoryBlocks.MINING_WELL_TILE.get(), RenderMiner::miningWell);
        event.registerBlockEntityRenderer(BCFactoryBlocks.DISTILLER_TILE.get(), RenderDistiller::new);
    }
}
