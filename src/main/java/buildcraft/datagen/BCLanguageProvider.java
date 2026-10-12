/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import net.minecraft.data.PackOutput;

import net.minecraftforge.common.data.LanguageProvider;

import buildcraft.BuildCraft;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;

/** Translations. Names are taken from the 1.12 translations (BuildCraft-Localization). */
public abstract class BCLanguageProvider extends LanguageProvider {
    protected BCLanguageProvider(PackOutput output, String locale) {
        super(output, BuildCraft.MODID, locale);
    }

    public static final class English extends BCLanguageProvider {
        public English(PackOutput output) {
            super(output, "en_us");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup.buildcraft.main", "BuildCraft");
            add(BCCoreItems.WRENCH.get(), "Wrench");
            add(BCCoreItems.GEAR_WOOD.get(), "Wood Gear");
            add(BCCoreItems.GEAR_STONE.get(), "Stone Gear");
            add(BCCoreItems.GEAR_IRON.get(), "Iron Gear");
            add(BCCoreItems.GEAR_GOLD.get(), "Gold Gear");
            add(BCCoreItems.GEAR_DIAMOND.get(), "Diamond Gear");
            add(BCCoreBlocks.ENGINE_REDSTONE.get(), "Redstone Engine");
            if (BCCoreBlocks.POWER_TESTER != null) {
                add(BCCoreBlocks.POWER_TESTER.get(), "Power Tester (Debug)");
            }
            add(BCTransportBlocks.PIPE_HOLDER.get(), "Pipe");
            add(BCTransportItems.PIPE_WOOD_ITEM.get(), "Wooden Transport Pipe");
            add(BCTransportItems.PIPE_COBBLESTONE_ITEM.get(), "Cobblestone Transport Pipe");
            add(BCTransportItems.PIPE_STONE_ITEM.get(), "Stone Transport Pipe");
            add(BCTransportItems.PIPE_QUARTZ_ITEM.get(), "Quartz Transport Pipe");
            add(BCTransportItems.PIPE_GOLD_ITEM.get(), "Golden Transport Pipe");
            add(BCTransportItems.PIPE_SANDSTONE_ITEM.get(), "Sandstone Transport Pipe");
            add(BCTransportItems.PIPE_IRON_ITEM.get(), "Iron Transport Pipe");
            add(BCTransportItems.PIPE_VOID_ITEM.get(), "Void Transport Pipe");
            add(BCTransportItems.PIPE_WOOD_POWER.get(), "Wooden Kinesis Pipe");
            add(BCTransportItems.PIPE_COBBLESTONE_POWER.get(), "Cobblestone Kinesis Pipe");
            add(BCTransportItems.PIPE_STONE_POWER.get(), "Stone Kinesis Pipe");
            add(BCTransportItems.PIPE_QUARTZ_POWER.get(), "Quartz Kinesis Pipe");
            add(BCTransportItems.PIPE_GOLD_POWER.get(), "Golden Kinesis Pipe");
            add(BCTransportItems.PIPE_SANDSTONE_POWER.get(), "Sandstone Kinesis Pipe");
        }
    }

    public static final class French extends BCLanguageProvider {
        public French(PackOutput output) {
            super(output, "fr_fr");
        }

        @Override
        protected void addTranslations() {
            add("itemGroup.buildcraft.main", "BuildCraft");
            add(BCCoreItems.WRENCH.get(), "Clé à molette");
            add(BCCoreItems.GEAR_WOOD.get(), "Engrenage de bois");
            add(BCCoreItems.GEAR_STONE.get(), "Engrenage de pierre");
            add(BCCoreItems.GEAR_IRON.get(), "Engrenage de fer");
            add(BCCoreItems.GEAR_GOLD.get(), "Engrenage d'or");
            add(BCCoreItems.GEAR_DIAMOND.get(), "Engrenage de diamant");
            add(BCCoreBlocks.ENGINE_REDSTONE.get(), "Moteur de redstone");
            if (BCCoreBlocks.POWER_TESTER != null) {
                add(BCCoreBlocks.POWER_TESTER.get(), "Testeur de puissance (debug)");
            }
            add(BCTransportBlocks.PIPE_HOLDER.get(), "Tuyau");
            add(BCTransportItems.PIPE_WOOD_ITEM.get(), "Tuyau de transport en bois");
            add(BCTransportItems.PIPE_COBBLESTONE_ITEM.get(), "Tuyau de transport en pierre");
            add(BCTransportItems.PIPE_STONE_ITEM.get(), "Tuyau de transport en roche");
            add(BCTransportItems.PIPE_QUARTZ_ITEM.get(), "Tuyau de transport en quartz");
            add(BCTransportItems.PIPE_GOLD_ITEM.get(), "Tuyau de transport en or");
            add(BCTransportItems.PIPE_SANDSTONE_ITEM.get(), "Tuyau de transport en grès");
            add(BCTransportItems.PIPE_IRON_ITEM.get(), "Tuyau de transport en fer");
            add(BCTransportItems.PIPE_VOID_ITEM.get(), "Tuyau d'évacuation");
            add(BCTransportItems.PIPE_WOOD_POWER.get(), "Tuyau cinétique en bois");
            add(BCTransportItems.PIPE_COBBLESTONE_POWER.get(), "Tuyau cinétique en pierre");
            add(BCTransportItems.PIPE_STONE_POWER.get(), "Tuyau cinétique en roche");
            add(BCTransportItems.PIPE_QUARTZ_POWER.get(), "Tuyau cinétique en quartz");
            add(BCTransportItems.PIPE_GOLD_POWER.get(), "Tuyau cinétique en or");
            add(BCTransportItems.PIPE_SANDSTONE_POWER.get(), "Tuyau cinétique en grès");
        }
    }
}
