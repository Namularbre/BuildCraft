/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.core.item.ItemWrench;

public final class BCCoreItems {
    public static final RegistryObject<ItemWrench> WRENCH =
        BCRegistries.ITEMS.register("wrench", () -> new ItemWrench(new Item.Properties()));

    public static final RegistryObject<Item> GEAR_WOOD = registerSimple("wood_gear");
    public static final RegistryObject<Item> GEAR_STONE = registerSimple("stone_gear");
    public static final RegistryObject<Item> GEAR_IRON = registerSimple("iron_gear");
    public static final RegistryObject<Item> GEAR_GOLD = registerSimple("gold_gear");
    public static final RegistryObject<Item> GEAR_DIAMOND = registerSimple("diamond_gear");

    private BCCoreItems() {}

    private static RegistryObject<Item> registerSimple(String name) {
        return BCRegistries.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
