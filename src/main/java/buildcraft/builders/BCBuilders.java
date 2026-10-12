/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import buildcraft.BuildCraft;
import buildcraft.builders.tile.TileQuarry;

/** The builders module: the quarry (and later the builder, filler and architect table). */
public final class BCBuilders {
    private BCBuilders() {}

    public static void init(FMLJavaModLoadingContext context) {
        context.registerConfig(
            ModConfig.Type.COMMON, BCBuildersConfig.SPEC, BuildCraft.MODID + "-builders-common.toml"
        );
        BCBuildersBlocks.init();
        context.getModEventBus().addListener(BCBuilders::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(
            () -> ForgeChunkManager.setForcedChunkLoadingCallback(BuildCraft.MODID, BCBuilders::validateTickets)
        );
    }

    /** Quarries keep the chunks of their area loaded: forget those of quarries that aren't there anymore. */
    private static void validateTickets(ServerLevel level, ForgeChunkManager.TicketHelper helper) {
        helper.getBlockTickets().keySet().forEach(owner -> {
            if (!(level.getBlockEntity(owner) instanceof TileQuarry)) {
                helper.removeAllTickets(owner);
            }
        });
    }
}
