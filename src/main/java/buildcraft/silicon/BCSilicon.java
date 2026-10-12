/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** The silicon module: lasers, and the tables they power. */
public final class BCSilicon {
    private BCSilicon() {}

    public static void init(FMLJavaModLoadingContext context) {
        BCSiliconBlocks.init();
        BCSiliconItems.init();
        BCSiliconMenus.init();
        context.getModEventBus().addListener(BCSilicon::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(BCSiliconRecipes::register);
    }
}
