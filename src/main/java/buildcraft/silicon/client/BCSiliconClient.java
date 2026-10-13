/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import net.minecraft.client.gui.screens.MenuScreens;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import buildcraft.BuildCraft;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.client.gui.GuiAdvancedCraftingTable;
import buildcraft.silicon.client.gui.GuiAssemblyTable;
import buildcraft.silicon.client.gui.GuiGate;
import buildcraft.silicon.client.render.RenderLaser;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.transport.client.model.PlugModels;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCSiliconClient {
    static {
        PlugModels.registerDynamic(PluggableGate.KeyPlugGate.class, GatePlugModel::boxes);
    }

    private BCSiliconClient() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCSiliconMenus.ASSEMBLY_TABLE.get(), GuiAssemblyTable::new);
            MenuScreens.register(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), GuiAdvancedCraftingTable::new);
            MenuScreens.register(BCSiliconMenus.GATE.get(), GuiGate::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCSiliconBlocks.LASER_TILE.get(), RenderLaser::new);
    }
}
