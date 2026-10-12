/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.block.BlockLaserTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileLaser;

public final class BCSiliconBlocks {
    public static final RegistryObject<BlockLaser> LASER = BCRegistries.BLOCKS.register(
        "laser", () -> new BlockLaser(machine())
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileLaser>> LASER_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "laser", () -> BlockEntityType.Builder.of(TileLaser::new, LASER.get()).build(null)
        );

    public static final RegistryObject<BlockLaserTable> ASSEMBLY_TABLE = BCRegistries.BLOCKS.register(
        "assembly_table", () -> new BlockLaserTable(machine(), TileAssemblyTable::new)
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileAssemblyTable>> ASSEMBLY_TABLE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "assembly_table",
            () -> BlockEntityType.Builder.of(TileAssemblyTable::new, ASSEMBLY_TABLE.get()).build(null)
        );

    static {
        BCRegistries.ITEMS.register("laser", () -> new BlockItem(LASER.get(), new Item.Properties()));
        BCRegistries.ITEMS.register(
            "assembly_table", () -> new BlockItem(ASSEMBLY_TABLE.get(), new Item.Properties())
        );
    }

    private BCSiliconBlocks() {}

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL);
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
