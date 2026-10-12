/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.energy.container.ContainerEngineIron;
import buildcraft.energy.container.ContainerEngineStone;

public final class BCEnergyMenus {
    public static final RegistryObject<MenuType<ContainerEngineStone>> ENGINE_STONE = BCRegistries.MENU_TYPES.register(
        "stirling_engine", () -> IForgeMenuType.create(ContainerEngineStone::new)
    );

    public static final RegistryObject<MenuType<ContainerEngineIron>> ENGINE_IRON = BCRegistries.MENU_TYPES.register(
        "combustion_engine", () -> IForgeMenuType.create(ContainerEngineIron::new)
    );

    private BCEnergyMenus() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
