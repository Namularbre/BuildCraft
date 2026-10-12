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
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.tile.TilePowerConsumerTester;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class KinesisGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final long INJECTED_PER_TICK = 2 * MjAPI.MJ;

    /** Power put into a wooden kinesis pipe goes through a stone kinesis pipe to the machine at the end, without any
     * power being created along the way. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void kinesisPipesCarryPower(GameTestHelper helper) {
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        BlockPos tester = new BlockPos(2, 1, 2);
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_POWER.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_POWER.get());
        helper.setBlock(tester, BCCoreBlocks.POWER_TESTER.get());

        long[] injected = { 0 };
        helper.onEachTick(() -> {
            // Act like an engine on the west side of the wooden pipe
            IMjReceiver receiver = helper.getBlockEntity(wood).getCapability(MjAPI.CAP_RECEIVER, Direction.WEST)
                .orElse(null);
            if (receiver != null && receiver.canReceive()) {
                long excess = receiver.receivePower(INJECTED_PER_TICK, false);
                injected[0] += INJECTED_PER_TICK - excess;
            }
        });

        helper.succeedWhen(() -> {
            long received = ((TilePowerConsumerTester) helper.getBlockEntity(tester)).getTotalReceived();
            helper.assertTrue(received >= 10 * MjAPI.MJ, "The tester should have received at least 10 MJ, got "
                + MjAPI.formatMj(received));
            helper.assertTrue(received <= injected[0], "More power came out (" + MjAPI.formatMj(received)
                + " MJ) than went in (" + MjAPI.formatMj(injected[0]) + " MJ)");
        });
    }

    /** Only the wooden kinesis pipe accepts power from outside. */
    @GameTest(template = EMPTY)
    public static void onlyWoodenKinesisPipesReceive(GameTestHelper helper) {
        BlockPos wood = new BlockPos(0, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_POWER.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_POWER.get());

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(
                canReceive(helper, wood), "A wooden kinesis pipe should accept power"
            );
            helper.assertFalse(canReceive(helper, stone), "A stone kinesis pipe shouldn't accept power");
            helper.succeed();
        });
    }

    /** As in 1.12, the redstone engine only powers redstone receivers (like wooden item pipes), so it can't push
     * power into kinesis pipes. */
    @GameTest(template = EMPTY)
    public static void redstoneEngineCantPowerKinesis(GameTestHelper helper) {
        BlockPos wood = new BlockPos(1, 1, 1);
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_POWER.get());
        placePipe(helper, wood.east(), BCTransportItems.PIPE_STONE_POWER.get());
        helper.setBlock(wood.east().south(), BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(wood.above(), BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(wood.above().north(), Blocks.REDSTONE_BLOCK);

        helper.runAfterDelay(80, () -> {
            long received = ((TilePowerConsumerTester) helper.getBlockEntity(wood.east().south())).getTotalReceived();
            helper.assertTrue(received == 0, "The redstone engine shouldn't power kinesis pipes");
            helper.succeed();
        });
    }

    private static boolean canReceive(GameTestHelper helper, BlockPos pos) {
        IMjReceiver receiver = helper.getBlockEntity(pos).getCapability(MjAPI.CAP_RECEIVER, Direction.UP).orElse(null);
        return receiver != null && receiver.canReceive();
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
    }
}
