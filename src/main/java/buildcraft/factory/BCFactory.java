/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import buildcraft.BuildCraft;

/** The factory module: tanks, pumps, mining wells, and refining oil. */
public final class BCFactory {
    private BCFactory() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, BCFactoryConfig.SPEC, BuildCraft.MODID + "-factory-common.toml");
        BCFactoryBlocks.init();
        BCFactoryItems.init();
        BCFactoryMenus.init();
    }
}
