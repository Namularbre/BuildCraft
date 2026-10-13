/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import net.minecraft.data.PackOutput;

import net.minecraftforge.common.data.LanguageProvider;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryItems;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.EnumRedstoneChipset;
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
            add(BCFactoryBlocks.MINING_WELL.get(), "Mining Well");
            add(BCFactoryBlocks.DISTILLER.get(), "Distiller");
            add(BCFactoryBlocks.CHUTE.get(), "Chute");
            add(BCSiliconBlocks.LASER.get(), "Laser");
            add(BCBuildersBlocks.QUARRY.get(), "Quarry");
            add(BCCoreBlocks.MARKER_VOLUME.get(), "Volume Marker");
            add(BCBuildersBlocks.FRAME.get(), "Frame");
            add(BCSiliconBlocks.ASSEMBLY_TABLE.get(), "Assembly Table");
            add(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get(), "Advanced Crafting Table");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.RED), "Redstone Chipset");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.IRON), "Iron Chipset");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.GOLD), "Gold Chipset");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.QUARTZ), "Quartz Chipset");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.DIAMOND), "Diamond Chipset");
            add("buildcraft.gui.laser_table.required", "Required: %s");
            add("buildcraft.gui.laser_table.stored", "Stored: %s");
            add("buildcraft.gui.laser_table.rate", "Rate: %s/t");
            add(BCFactoryBlocks.WATER_GEL.get(), "Water Gel");
            add(BCFactoryItems.WATER_GEL.get(), "Water Gel");
            add(BCFactoryItems.GEL.get(), "Gel");
            add(BCFactoryBlocks.AUTO_WORKBENCH_ITEMS.get(), "Auto Workbench");
            add(BCFactoryBlocks.FLOOD_GATE.get(), "Flood Gate");
            add(BCFactoryBlocks.HEAT_EXCHANGE.get(), "Heat Exchanger");
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
            add(BCTransportItems.PIPE_DIAMOND_ITEM.get(), "Diamond Transport Pipe");
            add(BCTransportItems.PIPE_STRUCTURE.get(), "Structure Pipe");
            add(BCTransportItems.PLUG_BLOCKER.get(), "Pipe Plug");
            add(BCTransportItems.PLUG_POWER_ADAPTOR.get(), "Power Adapter");
            add(BCTransportItems.PIPE_DIAMOND_FLUID.get(), "Diamond Fluid Pipe");
            add("buildcraft.gui.diamond_pipe", "Filters");
            add("itemGroup.buildcraft.facades", "BuildCraft Facades");
            add("buildcraft.facade", "Facade: %s");
            add("buildcraft.facade.hollow", "Hollow Facade: %s");
            add("buildcraft.facade.phased", "Phased Facade");
            add("buildcraft.facade.unknown", "Unknown");
            add("item.buildcraft.plug_facade", "Facade");
            add("buildcraft.gate.trigger.pipe.containsItems", "Items Traversing");
            add("buildcraft.gate.trigger.pipe.containsFluids", "Fluid Traversing");
            add("buildcraft.gate.trigger.pipe.requestsEnergy", "Power Requested");
            add("buildcraft.gate.action.pipe.direction", "Face the %s side");
            add("buildcraft.gate.trigger.light.bright", "Bright");
            add("buildcraft.gate.trigger.light.dark", "Dark");
            add("buildcraft.gate.action.pulsar.constant", "Power Pulsar");
            add("buildcraft.gate.action.pulsar.single", "Single Power Pulse");
            add("buildcraft.lens.lens", "%s Lens");
            add("buildcraft.lens.filter", "%s Filter");
            add("buildcraft.lens.clear", "Clear");
            add("item.buildcraft.plug_pulsar", "Pulsar");
            add("item.buildcraft.plug_light_sensor", "Light Sensor");
            add("buildcraft.gate.name.basic", "Basic Gate");
            add("buildcraft.gate.name", "%s %s Gate");
            add("buildcraft.gate.name.modified", "%s (%s)");
            add("buildcraft.gate.material.iron", "Iron");
            add("buildcraft.gate.material.nether_brick", "Nether Brick");
            add("buildcraft.gate.material.gold", "Gold");
            add("buildcraft.gate.logic.and", "AND");
            add("buildcraft.gate.logic.or", "OR");
            add("buildcraft.gate.modifier.lapis", "Lapis");
            add("buildcraft.gate.modifier.quartz", "Quartz");
            add("buildcraft.gate.modifier.diamond", "Diamond");
            add("buildcraft.gate.slots", "%s Slots");
            add("buildcraft.gate.params", "%s Slot Parameters");
            add("buildcraft.gate.params.trigger", "%s Trigger Parameters");
            add("buildcraft.gate.params.action", "%s Action Parameters");
            add("buildcraft.gate.side", "§7on the %s§7 side");
            add("buildcraft.gate.connection", "Group with the pair below");
            add("buildcraft.direction.down", "bottom");
            add("buildcraft.direction.up", "top");
            add("buildcraft.direction.north", "north");
            add("buildcraft.direction.south", "south");
            add("buildcraft.direction.west", "west");
            add("buildcraft.direction.east", "east");
            add("buildcraft.gate.trigger.true", "Always On");
            add("buildcraft.gate.trigger.redstone.input.active", "Redstone Signal On");
            add("buildcraft.gate.trigger.redstone.input.inactive", "Redstone Signal Off");
            add("buildcraft.gate.trigger.machine.scheduled", "Has Work");
            add("buildcraft.gate.trigger.machine.done", "Work Done");
            add("buildcraft.gate.trigger.machine.energyStored.high", "High Power Stored");
            add("buildcraft.gate.trigger.machine.energyStored.low", "Low Power Stored");
            add("buildcraft.gate.trigger.engine.blue", "Engine Blue");
            add("buildcraft.gate.trigger.engine.green", "Engine Green");
            add("buildcraft.gate.trigger.engine.yellow", "Engine Yellow");
            add("buildcraft.gate.trigger.engine.red", "Engine Red");
            add("buildcraft.gate.trigger.engine.overheat", "Engine Overheat");
            add("buildcraft.gate.trigger.inventory.empty", "Inventory Empty");
            add("buildcraft.gate.trigger.inventory.contains", "Items in Inventory");
            add("buildcraft.gate.trigger.inventory.space", "Space in Inventory");
            add("buildcraft.gate.trigger.inventory.full", "Inventory Full");
            add("buildcraft.gate.trigger.inventorylevel.below", "Contains < %s%%");
            add("buildcraft.gate.trigger.fluid.empty", "Tank Empty");
            add("buildcraft.gate.trigger.fluid.contains", "Fluid in Tank");
            add("buildcraft.gate.trigger.fluid.space", "Space for Fluid");
            add("buildcraft.gate.trigger.fluid.full", "Tank Full");
            add("buildcraft.gate.trigger.fluidlevel.below", "Contains < %s%%");
            add("buildcraft.gate.action.redstone.signal", "Redstone Signal");
            add("buildcraft.gate.action.machine.on", "On");
            add("buildcraft.gate.action.machine.off", "Off");
            add("buildcraft.gate.action.machine.loop", "Loop");
            add("buildcraft.gate.parameter.redstone.gateSideOnly", "Gate Side Only");
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
            add(BCFactoryBlocks.MINING_WELL.get(), "Puits de mine");
            add(BCFactoryBlocks.DISTILLER.get(), "Distillateur");
            add(BCFactoryBlocks.CHUTE.get(), "Goulotte");
            add(BCSiliconBlocks.LASER.get(), "Laser");
            add(BCBuildersBlocks.QUARRY.get(), "Carrière");
            add(BCCoreBlocks.MARKER_VOLUME.get(), "Marqueur de volume");
            add(BCBuildersBlocks.FRAME.get(), "Cadre");
            add(BCSiliconBlocks.ASSEMBLY_TABLE.get(), "Table d'assemblage");
            add(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get(), "Table de fabrication avancée");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.RED), "Circuit en redstone");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.IRON), "Circuit en fer");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.GOLD), "Circuit en or");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.QUARTZ), "Circuit en quartz");
            add(BCSiliconItems.chipset(EnumRedstoneChipset.DIAMOND), "Circuit en diamant");
            add("buildcraft.gui.laser_table.required", "Requis : %s");
            add("buildcraft.gui.laser_table.stored", "Stocké : %s");
            add("buildcraft.gui.laser_table.rate", "Débit : %s/t");
            add(BCFactoryBlocks.WATER_GEL.get(), "Gel d'eau");
            add(BCFactoryItems.WATER_GEL.get(), "Gel d'eau");
            add(BCFactoryItems.GEL.get(), "Gel");
            add(BCFactoryBlocks.AUTO_WORKBENCH_ITEMS.get(), "Établi automatique");
            add(BCFactoryBlocks.FLOOD_GATE.get(), "Vanne d'inondation");
            add(BCFactoryBlocks.HEAT_EXCHANGE.get(), "Échangeur de chaleur");
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
            add(BCTransportItems.PIPE_DIAMOND_ITEM.get(), "Tuyau de transport en diamant");
            add(BCTransportItems.PIPE_STRUCTURE.get(), "Tuyau de structure");
            add(BCTransportItems.PLUG_BLOCKER.get(), "Bouchon de tuyau");
            add(BCTransportItems.PLUG_POWER_ADAPTOR.get(), "Adaptateur d'énergie");
            add(BCTransportItems.PIPE_DIAMOND_FLUID.get(), "Tuyau à fluide en diamant");
            add("buildcraft.gui.diamond_pipe", "Filtres");
            add("itemGroup.buildcraft.facades", "Façades BuildCraft");
            add("buildcraft.facade", "Façade : %s");
            add("buildcraft.facade.hollow", "Façade creuse : %s");
            add("buildcraft.facade.phased", "Façade à phases");
            add("buildcraft.facade.unknown", "Inconnue");
            add("item.buildcraft.plug_facade", "Façade");
            add("buildcraft.gate.trigger.pipe.containsItems", "Objets en transit");
            add("buildcraft.gate.trigger.pipe.containsFluids", "Fluide en transit");
            add("buildcraft.gate.trigger.pipe.requestsEnergy", "Énergie demandée");
            add("buildcraft.gate.action.pipe.direction", "Tourner vers le côté %s");
            add("buildcraft.gate.trigger.light.bright", "Lumineux");
            add("buildcraft.gate.trigger.light.dark", "Sombre");
            add("buildcraft.gate.action.pulsar.constant", "Pulsar actif");
            add("buildcraft.gate.action.pulsar.single", "Impulsion unique");
            add("buildcraft.lens.lens", "Lentille (%s)");
            add("buildcraft.lens.filter", "Filtre (%s)");
            add("buildcraft.lens.clear", "incolore");
            add("item.buildcraft.plug_pulsar", "Pulsar");
            add("item.buildcraft.plug_light_sensor", "Capteur de lumière");
            add("buildcraft.gate.name.basic", "Porte basique");
            add("buildcraft.gate.name", "Porte %2$s en %1$s");
            add("buildcraft.gate.name.modified", "%s (%s)");
            add("buildcraft.gate.material.iron", "fer");
            add("buildcraft.gate.material.nether_brick", "brique du Nether");
            add("buildcraft.gate.material.gold", "or");
            add("buildcraft.gate.logic.and", "ET");
            add("buildcraft.gate.logic.or", "OU");
            add("buildcraft.gate.modifier.lapis", "lapis");
            add("buildcraft.gate.modifier.quartz", "quartz");
            add("buildcraft.gate.modifier.diamond", "diamant");
            add("buildcraft.gate.slots", "%s emplacements");
            add("buildcraft.gate.params", "%s paramètres par emplacement");
            add("buildcraft.gate.params.trigger", "%s paramètres de déclencheur");
            add("buildcraft.gate.params.action", "%s paramètres d'action");
            add("buildcraft.gate.side", "§7du côté %s§7");
            add("buildcraft.gate.connection", "Grouper avec la paire du dessous");
            add("buildcraft.direction.down", "bas");
            add("buildcraft.direction.up", "haut");
            add("buildcraft.direction.north", "nord");
            add("buildcraft.direction.south", "sud");
            add("buildcraft.direction.west", "ouest");
            add("buildcraft.direction.east", "est");
            add("buildcraft.gate.trigger.true", "Toujours actif");
            add("buildcraft.gate.trigger.redstone.input.active", "Signal de redstone actif");
            add("buildcraft.gate.trigger.redstone.input.inactive", "Signal de redstone inactif");
            add("buildcraft.gate.trigger.machine.scheduled", "A du travail");
            add("buildcraft.gate.trigger.machine.done", "Travail terminé");
            add("buildcraft.gate.trigger.machine.energyStored.high", "Énergie stockée haute");
            add("buildcraft.gate.trigger.machine.energyStored.low", "Énergie stockée basse");
            add("buildcraft.gate.trigger.engine.blue", "Moteur bleu");
            add("buildcraft.gate.trigger.engine.green", "Moteur vert");
            add("buildcraft.gate.trigger.engine.yellow", "Moteur jaune");
            add("buildcraft.gate.trigger.engine.red", "Moteur rouge");
            add("buildcraft.gate.trigger.engine.overheat", "Moteur en surchauffe");
            add("buildcraft.gate.trigger.inventory.empty", "Inventaire vide");
            add("buildcraft.gate.trigger.inventory.contains", "Objets dans l'inventaire");
            add("buildcraft.gate.trigger.inventory.space", "Place dans l'inventaire");
            add("buildcraft.gate.trigger.inventory.full", "Inventaire plein");
            add("buildcraft.gate.trigger.inventorylevel.below", "Contient < %s%%");
            add("buildcraft.gate.trigger.fluid.empty", "Réservoir vide");
            add("buildcraft.gate.trigger.fluid.contains", "Fluide dans le réservoir");
            add("buildcraft.gate.trigger.fluid.space", "Place pour du fluide");
            add("buildcraft.gate.trigger.fluid.full", "Réservoir plein");
            add("buildcraft.gate.trigger.fluidlevel.below", "Contient < %s%%");
            add("buildcraft.gate.action.redstone.signal", "Signal de redstone");
            add("buildcraft.gate.action.machine.on", "Marche");
            add("buildcraft.gate.action.machine.off", "Arrêt");
            add("buildcraft.gate.action.machine.loop", "Boucle");
            add("buildcraft.gate.parameter.redstone.gateSideOnly", "Côté de la porte uniquement");
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
