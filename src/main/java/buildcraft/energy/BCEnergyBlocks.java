/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.engine.BlockEngineBase;
import buildcraft.lib.registry.BCRegistries;

import buildcraft.energy.tile.TileEngineStone;

public final class BCEnergyBlocks {
    public static final RegistryObject<BlockEngineBase> ENGINE_STONE = BCRegistries.BLOCKS.register(
        "stirling_engine", () -> new BlockEngineBase(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f).sound(SoundType.STONE)
                .requiresCorrectToolForDrops(),
            BCEnergyBlocks.ENGINE_STONE_TILE
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileEngineStone>> ENGINE_STONE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "stirling_engine", () -> BlockEntityType.Builder.of(TileEngineStone::new, ENGINE_STONE.get()).build(null)
        );

    static {
        BCRegistries.ITEMS.register("stirling_engine", () -> new BlockItem(ENGINE_STONE.get(), new Item.Properties()));
    }

    private BCEnergyBlocks() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
