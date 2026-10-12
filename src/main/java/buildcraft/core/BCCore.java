/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.BuildCraft;
import buildcraft.lib.registry.BCRegistries;

/** The core module: tools, gears, and the redstone engine. */
public final class BCCore {
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = BCRegistries.CREATIVE_MODE_TABS.register(
        "main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BuildCraft.MODID + ".main"))
            .icon(() -> new ItemStack(BCCoreItems.WRENCH.get()))
            .displayItems((params, output) -> {
                // Every item, in registration order
                BCRegistries.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            })
            .build()
    );

    private BCCore() {}

    public static void init() {
        BCCoreBlocks.init();
        BCCoreItems.init();
    }
}
