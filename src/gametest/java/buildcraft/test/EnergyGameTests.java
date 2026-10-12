/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.engine.BlockEngineBase;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.tile.TilePowerConsumerTester;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.tile.TileEngineStone;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class EnergyGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** The full survival chain: a Stirling engine burning coal powers a wooden kinesis pipe, and kinesis pipes carry
     * the power to a machine. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void stirlingEnginePowersKinesisPipes(GameTestHelper helper) {
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        BlockPos tester = new BlockPos(2, 1, 2);
        BlockPos engine = wood.above();
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_POWER.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_POWER.get());
        helper.setBlock(tester, BCCoreBlocks.POWER_TESTER.get());

        helper.runAfterDelay(2, () -> {
            helper.setBlock(engine, BCEnergyBlocks.ENGINE_STONE.get());
            ((TileEngineStone) helper.getBlockEntity(engine)).invFuel.insertItem(
                0, new ItemStack(Items.COAL, 4), false
            );
            helper.setBlock(engine.north(), Blocks.REDSTONE_BLOCK);
            helper.assertBlockProperty(engine, BlockEngineBase.FACING, Direction.DOWN);

            helper.succeedWhen(() -> {
                long received = ((TilePowerConsumerTester) helper.getBlockEntity(tester)).getTotalReceived();
                helper.assertTrue(received >= MjAPI.MJ, "The tester should have received power, got "
                    + MjAPI.formatMj(received) + " MJ");
            });
        });
    }

    /** Burning a lava bucket leaves the empty bucket in the fuel slot. */
    @GameTest(template = EMPTY)
    public static void stirlingEngineKeepsEmptyBuckets(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_STONE.get());
        TileEngineStone tile = (TileEngineStone) helper.getBlockEntity(engine);
        tile.invFuel.insertItem(0, new ItemStack(Items.LAVA_BUCKET), false);
        helper.setBlock(engine.west(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(tile.isBurning(), "The engine should be burning the lava");
            ItemStack left = tile.invFuel.getStackInSlot(0);
            helper.assertTrue(left.is(Items.BUCKET), "An empty bucket should be left, found " + left);
            helper.succeed();
        });
    }

    /** Only things that burn go in the fuel slot. */
    @GameTest(template = EMPTY)
    public static void stirlingEngineOnlyAcceptsFuel(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_STONE.get());
        TileEngineStone tile = (TileEngineStone) helper.getBlockEntity(engine);

        ItemStack rejected = tile.invFuel.insertItem(0, new ItemStack(Items.DIRT), false);
        helper.assertTrue(rejected.getCount() == 1, "Dirt shouldn't be accepted as fuel");
        ItemStack accepted = tile.invFuel.insertItem(0, new ItemStack(Items.COAL), false);
        helper.assertTrue(accepted.isEmpty(), "Coal should be accepted as fuel");
        helper.succeed();
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
    }
}
