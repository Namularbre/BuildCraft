/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.factory.item.ItemWaterGel;

public final class BCFactoryItems {
    /** Thrown into water, makes it set into gel. */
    public static final RegistryObject<ItemWaterGel> WATER_GEL =
        BCRegistries.ITEMS.register("water_gel_spawn", () -> new ItemWaterGel(new Item.Properties()));
    /** Dropped by set gel: turns back into water in a bucket. */
    public static final RegistryObject<Item> GEL =
        BCRegistries.ITEMS.register("gel", () -> new Item(new Item.Properties()));

    private BCFactoryItems() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
