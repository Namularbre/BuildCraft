/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.factory.block.BlockTank;
import buildcraft.factory.tile.TileTank;

public final class BCFactoryBlocks {
    public static final RegistryObject<BlockTank> TANK = BCRegistries.BLOCKS.register(
        "tank", () -> new BlockTank(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileTank>> TANK_TILE = BCRegistries.BLOCK_ENTITY_TYPES.register(
        "tank", () -> BlockEntityType.Builder.of(TileTank::new, TANK.get()).build(null)
    );

    static {
        BCRegistries.ITEMS.register("tank", () -> new BlockItem(TANK.get(), new Item.Properties()));
    }

    private BCFactoryBlocks() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
