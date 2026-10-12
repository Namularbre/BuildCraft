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
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.factory.BCFactoryBlocks;
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
            add(BCEnergyBlocks.ENGINE_STONE.get(), "Stirling Engine");
            add(BCEnergyBlocks.ENGINE_IRON.get(), "Combustion Engine");
            add(BCFactoryBlocks.TANK.get(), "Tank");
            add(BCFactoryBlocks.PUMP.get(), "Pump");
            add(BCFactoryBlocks.TUBE.get(), "Tube");
            add("buildcraft.gui.tank.fuel", "Fuel");
            add("buildcraft.gui.tank.coolant", "Coolant");
            add("buildcraft.gui.tank.residue", "Residue");
            add("buildcraft.gui.tank.empty", "Empty");
            add("buildcraft.gui.tank.amount", "%s / %s mB");
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
            add(BCTransportItems.PIPE_WOOD_FLUID.get(), "Wooden Fluid Pipe");
            add(BCTransportItems.PIPE_COBBLESTONE_FLUID.get(), "Cobblestone Fluid Pipe");
            add(BCTransportItems.PIPE_STONE_FLUID.get(), "Stone Fluid Pipe");
            add(BCTransportItems.PIPE_QUARTZ_FLUID.get(), "Quartz Fluid Pipe");
            add(BCTransportItems.PIPE_GOLD_FLUID.get(), "Golden Fluid Pipe");
            add(BCTransportItems.PIPE_SANDSTONE_FLUID.get(), "Sandstone Fluid Pipe");
            add(BCTransportItems.PIPE_IRON_FLUID.get(), "Iron Fluid Pipe");
            add(BCTransportItems.PIPE_VOID_FLUID.get(), "Void Fluid Pipe");
            add(BCTransportItems.PIPE_SEALANT.get(), "Pipe Sealant");

            add("buildcraft.fluid.heat_0", "%s (§bCool§r)");
            add("buildcraft.fluid.heat_1", "%s (§6Hot§r)");
            add("buildcraft.fluid.heat_2", "%s (§cSearing§r)");
            add("item.buildcraft.fluid_bucket", "Bucket of %s");
            add("fluid.buildcraft.oil", "Oil");
            add("fluid.buildcraft.oil_residue", "Residue");
            add("fluid.buildcraft.oil_heavy", "Heavy Oil");
            add("fluid.buildcraft.oil_dense", "Dense Oil");
            add("fluid.buildcraft.oil_distilled", "Distilled Oil");
            add("fluid.buildcraft.fuel_dense", "Dense Fuel");
            add("fluid.buildcraft.fuel_mixed_heavy", "Mixed Heavy Fuels");
            add("fluid.buildcraft.fuel_light", "Light Fuel");
            add("fluid.buildcraft.fuel_mixed_light", "Mixed Light Fuels");
            add("fluid.buildcraft.fuel_gaseous", "Gaseous Fuel");
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
            add(BCEnergyBlocks.ENGINE_STONE.get(), "Moteur à vapeur");
            add(BCEnergyBlocks.ENGINE_IRON.get(), "Moteur à combustion");
            add(BCFactoryBlocks.TANK.get(), "Réservoir");
            add(BCFactoryBlocks.PUMP.get(), "Pompe");
            add(BCFactoryBlocks.TUBE.get(), "Tube");
            add("buildcraft.gui.tank.fuel", "Combustible");
            add("buildcraft.gui.tank.coolant", "Refroidissant");
            add("buildcraft.gui.tank.residue", "Résidu");
            add("buildcraft.gui.tank.empty", "Vide");
            add("buildcraft.gui.tank.amount", "%s / %s mB");
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
            add(BCTransportItems.PIPE_WOOD_FLUID.get(), "Tuyau à fluide en bois");
            add(BCTransportItems.PIPE_COBBLESTONE_FLUID.get(), "Tuyau à fluide en pierre");
            add(BCTransportItems.PIPE_STONE_FLUID.get(), "Tuyau à fluide en roche");
            add(BCTransportItems.PIPE_QUARTZ_FLUID.get(), "Tuyau à fluide en quartz");
            add(BCTransportItems.PIPE_GOLD_FLUID.get(), "Tuyau à fluide en or");
            add(BCTransportItems.PIPE_SANDSTONE_FLUID.get(), "Tuyau à fluide en grès");
            add(BCTransportItems.PIPE_IRON_FLUID.get(), "Tuyau à fluide en fer");
            add(BCTransportItems.PIPE_VOID_FLUID.get(), "Tuyau d'évacuation de fluide");
            add(BCTransportItems.PIPE_SEALANT.get(), "Enduit étanche");

            add("buildcraft.fluid.heat_0", "%s (§bFroid§r)");
            add("buildcraft.fluid.heat_1", "%s (§6Chaud§r)");
            add("buildcraft.fluid.heat_2", "%s (§cBrûlant§r)");
            add("item.buildcraft.fluid_bucket", "Seau de %s");
            add("fluid.buildcraft.oil", "Pétrole");
            add("fluid.buildcraft.oil_residue", "Résidu");
            add("fluid.buildcraft.oil_heavy", "Pétrole lourd");
            add("fluid.buildcraft.oil_dense", "Pétrole dense");
            add("fluid.buildcraft.oil_distilled", "Pétrole distillé");
            add("fluid.buildcraft.fuel_dense", "Carburant dense");
            add("fluid.buildcraft.fuel_mixed_heavy", "Carburants lourds mélangés");
            add("fluid.buildcraft.fuel_light", "Essence");
            add("fluid.buildcraft.fuel_mixed_light", "Carburants légers mélangés");
            add("fluid.buildcraft.fuel_gaseous", "Carburant gazeux");
        }
    }
}
