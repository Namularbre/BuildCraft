/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.builders.container.ContainerFiller;

public final class BCBuildersMenus {
    public static final RegistryObject<MenuType<ContainerFiller>> FILLER =
        BCRegistries.MENU_TYPES.register("filler", () -> IForgeMenuType.create(ContainerFiller::new));

    private BCBuildersMenus() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
