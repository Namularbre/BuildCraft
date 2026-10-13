/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.block.BlockFrame;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileQuarry;

public final class BCBuildersBlocks {
    public static final RegistryObject<BlockQuarry> QUARRY = BCRegistries.BLOCKS.register(
        "quarry", () -> new BlockQuarry(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.ANVIL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileQuarry>> QUARRY_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "quarry", () -> BlockEntityType.Builder.of(TileQuarry::new, QUARRY.get()).build(null)
        );

    /** Placed by quarries around their area, and removed with them. */
    public static final RegistryObject<BlockFrame> FRAME = BCRegistries.BLOCKS.register(
        "frame", () -> new BlockFrame(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.METAL)
                .noOcclusion().noLootTable()
        )
    );

    public static final RegistryObject<BlockFiller> FILLER = BCRegistries.BLOCKS.register(
        "filler", () -> new BlockFiller(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileFiller>> FILLER_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "filler", () -> BlockEntityType.Builder.of(TileFiller::new, FILLER.get()).build(null)
        );

    static {
        BCRegistries.ITEMS.register("quarry", () -> new BlockItem(QUARRY.get(), new Item.Properties()));
        BCRegistries.ITEMS.register("filler", () -> new BlockItem(FILLER.get(), new Item.Properties()));
    }

    private BCBuildersBlocks() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
