/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.lib.registry.BCRegistries;

import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemGateCopier;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.silicon.item.ItemPluggableLens;
import buildcraft.silicon.item.ItemPluggablePulsar;

public final class BCSiliconItems {
    public static final Map<EnumRedstoneChipset, RegistryObject<Item>> CHIPSETS =
        new EnumMap<>(EnumRedstoneChipset.class);

    static {
        for (EnumRedstoneChipset chipset : EnumRedstoneChipset.values()) {
            CHIPSETS.put(chipset,
                BCRegistries.ITEMS.register(chipset.getItemName(), () -> new Item(new Item.Properties())));
        }
    }

    /** Every gate variant has its own item. */
    public static final Map<GateVariant, RegistryObject<ItemPluggableGate>> GATES = new LinkedHashMap<>();

    static {
        for (GateVariant variant : GateVariant.VARIANTS) {
            GATES.put(variant, BCRegistries.ITEMS.register(variant.getItemName(),
                () -> new ItemPluggableGate(new Item.Properties(), variant)));
        }
    }

    public static final RegistryObject<ItemGateCopier> GATE_COPIER =
        BCRegistries.ITEMS.register("gate_copier", () -> new ItemGateCopier(new Item.Properties()));
    public static final RegistryObject<ItemPluggablePulsar> PLUG_PULSAR =
        BCRegistries.ITEMS.register("plug_pulsar", () -> new ItemPluggablePulsar(new Item.Properties()));
    public static final RegistryObject<ItemPluggableSimple> PLUG_LIGHT_SENSOR = BCRegistries.ITEMS.register(
        "plug_light_sensor", () -> new ItemPluggableSimple(new Item.Properties(), BCSiliconPlugs.LIGHT_SENSOR)
    );

    /** The lenses and filters of every colour, then the clear ones. */
    public static final Map<String, RegistryObject<ItemPluggableLens>> LENSES = new LinkedHashMap<>();

    static {
        List<DyeColor> colours = new ArrayList<>(List.of(DyeColor.values()));
        colours.add(null);
        for (DyeColor colour : colours) {
            for (boolean filter : new boolean[] { false, true }) {
                String name = ItemPluggableLens.getItemName(colour, filter);
                LENSES.put(name, BCRegistries.ITEMS.register(name,
                    () -> new ItemPluggableLens(new Item.Properties(), colour, filter)));
            }
        }
    }

    public static final RegistryObject<ItemPluggableFacade> PLUG_FACADE =
        BCRegistries.ITEMS.register("plug_facade", () -> new ItemPluggableFacade(new Item.Properties()));

    private BCSiliconItems() {}

    public static ItemPluggableLens lens(@Nullable DyeColor colour, boolean isFilter) {
        return LENSES.get(ItemPluggableLens.getItemName(colour, isFilter)).get();
    }

    public static Item chipset(EnumRedstoneChipset chipset) {
        return CHIPSETS.get(chipset).get();
    }

    public static ItemPluggableGate gate(GateVariant variant) {
        return GATES.get(variant).get();
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
