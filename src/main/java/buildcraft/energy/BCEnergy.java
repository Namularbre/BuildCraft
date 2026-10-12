/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import buildcraft.BuildCraft;

/** The energy module: engines that burn fuel, and oil. */
public final class BCEnergy {
    private BCEnergy() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, BCEnergyConfig.SPEC, BuildCraft.MODID + "-energy-common.toml");
        BCEnergyBlocks.init();
        BCEnergyFluids.init();
        BCEnergyMenus.init();
        context.getModEventBus().addListener(BCEnergy::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BCEnergyFuels::register);
    }
}
