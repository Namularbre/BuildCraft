/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core;

import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.BCLib;
import buildcraft.lib.engine.BlockEngineBase;
import buildcraft.lib.registry.BCRegistries;

import buildcraft.core.block.BlockMarkerVolume;
import buildcraft.core.block.BlockPowerConsumerTester;
import buildcraft.core.tile.TileEngineRedstone;
import buildcraft.core.tile.TileMarkerVolume;
import buildcraft.core.tile.TilePowerConsumerTester;

public final class BCCoreBlocks {
    public static final RegistryObject<BlockEngineBase> ENGINE_REDSTONE = registerWithItem(
        "redstone_engine", () -> new BlockEngineBase(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD),
            BCCoreBlocks.ENGINE_REDSTONE_TILE
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileEngineRedstone>> ENGINE_REDSTONE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "redstone_engine", () -> BlockEntityType.Builder.of(TileEngineRedstone::new, ENGINE_REDSTONE.get())
                .build(null)
        );

    /** Development-only: accepts any MJ, and tells the player how much it received. Null outside of development. */
    public static final RegistryObject<BlockPowerConsumerTester> POWER_TESTER = BCLib.DEV ? registerWithItem(
        "power_tester", () -> new BlockPowerConsumerTester(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5f).sound(SoundType.METAL)
        )
    ) : null;

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TilePowerConsumerTester>> POWER_TESTER_TILE = BCLib.DEV
        ? BCRegistries.BLOCK_ENTITY_TYPES.register(
            "power_tester", () -> BlockEntityType.Builder.of(TilePowerConsumerTester::new, POWER_TESTER.get())
                .build(null)
        ) : null;

    public static final RegistryObject<BlockMarkerVolume> MARKER_VOLUME = registerWithItem(
        "marker_volume", () -> new BlockMarkerVolume(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).instabreak().sound(SoundType.WOOD).lightLevel(s -> 3)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileMarkerVolume>> MARKER_VOLUME_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "marker_volume", () -> BlockEntityType.Builder.of(TileMarkerVolume::new, MARKER_VOLUME.get()).build(null)
        );

    private BCCoreBlocks() {}

    private static <B extends Block> RegistryObject<B> registerWithItem(String name, Supplier<B> block) {
        RegistryObject<B> registered = BCRegistries.BLOCKS.register(name, block);
        BCRegistries.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
