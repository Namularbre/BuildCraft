/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import buildcraft.lib.BCLib;

import buildcraft.core.BCCore;
import buildcraft.energy.BCEnergy;
import buildcraft.factory.BCFactory;
import buildcraft.transport.BCTransport;

@Mod(BuildCraft.MODID)
public class BuildCraft {
    public static final String MODID = "buildcraft";

    public BuildCraft(FMLJavaModLoadingContext context) {
        BCLib.init(context);
        BCCore.init();
        BCTransport.init(context.getModEventBus());
        BCEnergy.init(context);
        BCFactory.init(context);
    }
}
