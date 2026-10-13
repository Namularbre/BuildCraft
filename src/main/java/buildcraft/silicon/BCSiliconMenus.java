/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import buildcraft.silicon.container.ContainerAssemblyTable;
import buildcraft.silicon.container.ContainerGate;

public final class BCSiliconMenus {
    public static final RegistryObject<MenuType<ContainerAssemblyTable>> ASSEMBLY_TABLE =
        BCRegistries.MENU_TYPES.register("assembly_table", () -> IForgeMenuType.create(ContainerAssemblyTable::new));

    public static final RegistryObject<MenuType<ContainerAdvancedCraftingTable>> ADVANCED_CRAFTING_TABLE =
        BCRegistries.MENU_TYPES.register(
            "advanced_crafting_table", () -> IForgeMenuType.create(ContainerAdvancedCraftingTable::new)
        );

    public static final RegistryObject<MenuType<ContainerGate>> GATE =
        BCRegistries.MENU_TYPES.register("gate", () -> IForgeMenuType.create(ContainerGate::new));

    private BCSiliconMenus() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
