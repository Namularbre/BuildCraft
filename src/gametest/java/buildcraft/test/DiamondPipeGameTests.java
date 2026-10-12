/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileTank;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class DiamondPipeGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos PIPE = new BlockPos(1, 1, 1);

    /** Items go to the side whose filters match them. */
    @GameTest(template = EMPTY)
    public static void diamondPipesSortItems(GameTestHelper helper) {
        helper.setBlock(PIPE.north(), Blocks.CHEST);
        helper.setBlock(PIPE.east(), Blocks.CHEST);
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        PipeBehaviourDiamond diamond = placePipe(helper, BCTransportItems.PIPE_DIAMOND_ITEM.get());
        setFilter(diamond, Direction.EAST, new ItemStack(Items.COBBLESTONE));
        setFilter(diamond, Direction.WEST, new ItemStack(Items.DIRT));

        helper.runAfterDelay(2, () -> {
            IInjectable injectable = helper.getBlockEntity(PIPE).getCapability(PipeApi.CAP_INJECTABLE, Direction.NORTH)
                .orElseThrow(() -> new GameTestAssertException("The pipe should accept items"));
            // Faster than the default speed (0.01 blocks per tick), so that the test doesn't take too long
            injectable.injectItem(new ItemStack(Items.COBBLESTONE, 4), true, Direction.NORTH, null, 0.1);
            injectable.injectItem(new ItemStack(Items.DIRT, 4), true, Direction.NORTH, null, 0.1);
        });
        helper.succeedWhen(() -> {
            assertCount(helper, PIPE.east(), Items.COBBLESTONE, 4);
            assertCount(helper, PIPE.west(), Items.DIRT, 4);
            assertCount(helper, PIPE.east(), Items.DIRT, 0);
            assertCount(helper, PIPE.west(), Items.COBBLESTONE, 0);
        });
    }

    /** Fluids go to the side whose filters (buckets) hold the same fluid. */
    @GameTest(template = EMPTY)
    public static void diamondPipesSortFluids(GameTestHelper helper) {
        helper.setBlock(PIPE.north(), BCFactoryBlocks.TANK.get());
        helper.setBlock(PIPE.east(), BCFactoryBlocks.TANK.get());
        helper.setBlock(PIPE.west(), BCFactoryBlocks.TANK.get());
        PipeBehaviourDiamond diamond = placePipe(helper, BCTransportItems.PIPE_DIAMOND_FLUID.get());
        setFilter(diamond, Direction.EAST, new ItemStack(Items.LAVA_BUCKET));
        setFilter(diamond, Direction.WEST, new ItemStack(Items.WATER_BUCKET));

        helper.onEachTick(() -> helper.getBlockEntity(PIPE)
            .getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH)
            .ifPresent(handler -> handler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE)));
        helper.succeedWhen(() -> {
            TileTank west = (TileTank) helper.getBlockEntity(PIPE.west());
            TileTank east = (TileTank) helper.getBlockEntity(PIPE.east());
            helper.assertTrue(west.tank.getFluidAmount() >= 500, "Water should go west, which has a water filter");
            helper.assertTrue(east.tank.isEmpty(), "Water shouldn't go east, which only takes lava");
        });
    }

    private static PipeBehaviourDiamond placePipe(GameTestHelper helper, Item pipeItem) {
        helper.setBlock(PIPE, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(PIPE);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return (PipeBehaviourDiamond) tile.getPipe().getBehaviour();
    }

    private static void setFilter(PipeBehaviourDiamond diamond, Direction side, ItemStack filter) {
        diamond.filters.setStackInSlot(PipeBehaviourDiamond.FILTERS_PER_SIDE * side.ordinal(), filter);
    }

    private static void assertCount(GameTestHelper helper, BlockPos chest, Item item, int expected) {
        int count = ((ChestBlockEntity) helper.getBlockEntity(chest)).countItem(item);
        helper.assertTrue(count == expected, "Expected " + expected + " " + item + " at " + chest + ", found " + count);
    }
}
