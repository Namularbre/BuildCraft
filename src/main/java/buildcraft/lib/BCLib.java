/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import buildcraft.api.mj.IMjToRfStatus;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.net.MessageManager;
import buildcraft.lib.registry.BCRegistries;

/** Sets up the shared library code that every other BuildCraft module builds on. */
public final class BCLib {
    /** True in development environments (or with -Dbuildcraft.dev=true), enabling debugging content. */
    public static final boolean DEV = !FMLEnvironment.production || Boolean.getBoolean("buildcraft.dev");

    private BCLib() {}

    public static void init(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.COMMON, BCLibConfig.SPEC);
        IMjToRfStatus.set(new BCLibConfig.MjToRfStatus());

        BCRegistries.register(modBus);
        modBus.addListener(MjAPI::registerCapabilities);
        modBus.addListener(BCLib::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MessageManager::registerMessages);
    }
}
