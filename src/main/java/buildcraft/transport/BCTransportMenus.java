/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.transport.container.ContainerDiamondPipe;
import buildcraft.transport.container.ContainerDiamondWoodPipe;
import buildcraft.transport.container.ContainerEmzuliPipe;

public final class BCTransportMenus {
    public static final RegistryObject<MenuType<ContainerDiamondPipe>> DIAMOND_PIPE =
        BCRegistries.MENU_TYPES.register("diamond_pipe", () -> IForgeMenuType.create(ContainerDiamondPipe::new));

    public static final RegistryObject<MenuType<ContainerDiamondWoodPipe>> DIAMOND_WOOD_PIPE =
        BCRegistries.MENU_TYPES.register("diamond_wood_pipe",
            () -> IForgeMenuType.create(ContainerDiamondWoodPipe::new));
    public static final RegistryObject<MenuType<ContainerEmzuliPipe>> EMZULI_PIPE =
        BCRegistries.MENU_TYPES.register("emzuli_pipe", () -> IForgeMenuType.create(ContainerEmzuliPipe::new));

    private BCTransportMenus() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
