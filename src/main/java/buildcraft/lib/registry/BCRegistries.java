/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.BuildCraft;

/** Every registry that BuildCraft adds content to. Each module registers its own entries into these. */
public final class BCRegistries {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, BuildCraft.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, BuildCraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BuildCraft.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, BuildCraft.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BuildCraft.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BuildCraft.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(ForgeRegistries.FLUIDS, BuildCraft.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(ForgeRegistries.FEATURES, BuildCraft.MODID);

    private BCRegistries() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        FEATURES.register(modBus);
    }
}
