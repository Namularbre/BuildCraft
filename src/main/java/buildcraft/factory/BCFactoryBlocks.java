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
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.factory.block.BlockAutoWorkbenchItems;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.block.BlockPump;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller;
import buildcraft.factory.tile.TileFloodGate;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TilePump;
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

    public static final RegistryObject<BlockPump> PUMP = BCRegistries.BLOCKS.register(
        "pump", () -> new BlockPump(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TilePump>> PUMP_TILE = BCRegistries.BLOCK_ENTITY_TYPES.register(
        "pump", () -> BlockEntityType.Builder.of(TilePump::new, PUMP.get()).build(null)
    );

    public static final RegistryObject<BlockMiningWell> MINING_WELL = BCRegistries.BLOCKS.register(
        "mining_well", () -> new BlockMiningWell(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileMiningWell>> MINING_WELL_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "mining_well", () -> BlockEntityType.Builder.of(TileMiningWell::new, MINING_WELL.get()).build(null)
        );

    public static final RegistryObject<BlockDistiller> DISTILLER = BCRegistries.BLOCKS.register(
        "distiller", () -> new BlockDistiller(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileDistiller>> DISTILLER_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "distiller", () -> BlockEntityType.Builder.of(TileDistiller::new, DISTILLER.get()).build(null)
        );

    public static final RegistryObject<BlockHeatExchange> HEAT_EXCHANGE = BCRegistries.BLOCKS.register(
        "heat_exchange", () -> new BlockHeatExchange(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileHeatExchange>> HEAT_EXCHANGE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "heat_exchange", () -> BlockEntityType.Builder.of(TileHeatExchange::new, HEAT_EXCHANGE.get()).build(null)
        );

    public static final RegistryObject<BlockChute> CHUTE = BCRegistries.BLOCKS.register(
        "chute", () -> new BlockChute(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileChute>> CHUTE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "chute", () -> BlockEntityType.Builder.of(TileChute::new, CHUTE.get()).build(null)
        );

    public static final RegistryObject<BlockFloodGate> FLOOD_GATE = BCRegistries.BLOCKS.register(
        "flood_gate", () -> new BlockFloodGate(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 10.0f).sound(SoundType.METAL)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileFloodGate>> FLOOD_GATE_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "flood_gate", () -> BlockEntityType.Builder.of(TileFloodGate::new, FLOOD_GATE.get()).build(null)
        );

    public static final RegistryObject<BlockAutoWorkbenchItems> AUTO_WORKBENCH_ITEMS = BCRegistries.BLOCKS.register(
        "autoworkbench_item", () -> new BlockAutoWorkbenchItems(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TileAutoWorkbenchItems>> AUTO_WORKBENCH_ITEMS_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "autoworkbench_item",
            () -> BlockEntityType.Builder.of(TileAutoWorkbenchItems::new, AUTO_WORKBENCH_ITEMS.get()).build(null)
        );

    /** Made by throwing water gel into water: it has no item, and drops gel. */
    public static final RegistryObject<BlockWaterGel> WATER_GEL = BCRegistries.BLOCKS.register(
        "water_gel", () -> new BlockWaterGel(
            BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(0.6f).sound(SoundType.SLIME_BLOCK)
        )
    );

    /** Unbreakable: it is removed with the machine above it. */
    public static final RegistryObject<BlockTube> TUBE = BCRegistries.BLOCKS.register(
        "tube", () -> new BlockTube(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3_600_000.0f).noLootTable()
                .pushReaction(PushReaction.BLOCK)
        )
    );

    static {
        BCRegistries.ITEMS.register("tank", () -> new BlockItem(TANK.get(), new Item.Properties()));
        BCRegistries.ITEMS.register("pump", () -> new BlockItem(PUMP.get(), new Item.Properties()));
        BCRegistries.ITEMS.register("mining_well", () -> new BlockItem(MINING_WELL.get(), new Item.Properties()));
        BCRegistries.ITEMS.register("distiller", () -> new BlockItem(DISTILLER.get(), new Item.Properties()));
        BCRegistries.ITEMS.register("chute", () -> new BlockItem(CHUTE.get(), new Item.Properties()));
        BCRegistries.ITEMS.register(
            "autoworkbench_item", () -> new BlockItem(AUTO_WORKBENCH_ITEMS.get(), new Item.Properties())
        );
        BCRegistries.ITEMS.register("flood_gate", () -> new BlockItem(FLOOD_GATE.get(), new Item.Properties()));
        BCRegistries.ITEMS.register(
            "heat_exchange", () -> new BlockItem(HEAT_EXCHANGE.get(), new Item.Properties())
        );
    }

    private BCFactoryBlocks() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
