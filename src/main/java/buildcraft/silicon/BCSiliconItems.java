/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemPluggableGate;

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

    private BCSiliconItems() {}

    public static Item chipset(EnumRedstoneChipset chipset) {
        return CHIPSETS.get(chipset).get();
    }

    public static ItemPluggableGate gate(GateVariant variant) {
        return GATES.get(variant).get();
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
