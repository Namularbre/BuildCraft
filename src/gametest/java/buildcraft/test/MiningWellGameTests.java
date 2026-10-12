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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileMiningWell;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class MiningWellGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";
    private static final BlockPos WELL = new BlockPos(2, 4, 2);
    private static final BlockPos CHEST = WELL.east();

    /** A powered mining well digs down to bedrock, puts what it mined in the chest next to it, then pulls its tube
     * back up. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void miningWellDigsDown(GameTestHelper helper) {
        helper.setBlock(WELL.below(), Blocks.DIRT);
        helper.setBlock(WELL.below(2), Blocks.STONE);
        helper.setBlock(WELL.below(3), Blocks.COBBLESTONE);
        helper.setBlock(WELL.below(4), Blocks.BEDROCK);
        helper.setBlock(WELL, BCFactoryBlocks.MINING_WELL.get());
        helper.setBlock(CHEST, Blocks.CHEST);
        TileMiningWell well = (TileMiningWell) helper.getBlockEntity(WELL);
        IMjReceiver receiver = well.getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(IllegalStateException::new);
        helper.onEachTick(() -> receiver.receivePower(50 * MjAPI.MJ, false));

        helper.succeedWhen(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(CHEST);
            assertCount(helper, chest, Items.DIRT, 1);
            // Stone drops cobblestone
            assertCount(helper, chest, Items.COBBLESTONE, 2);
            helper.assertBlockPresent(Blocks.BEDROCK, WELL.below(4));
            helper.assertTrue(well.isComplete(), "The mining well should have nothing left to mine");
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), WELL.below());
        });
    }

    /** The tube goes down through air to the first block to mine, and is removed with the mining well. */
    @GameTest(template = EMPTY)
    public static void tubeIsRemovedWithTheMiningWell(GameTestHelper helper) {
        helper.setBlock(WELL.below(3), Blocks.DIRT);
        helper.setBlock(WELL, BCFactoryBlocks.MINING_WELL.get());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCFactoryBlocks.TUBE.get(), WELL.below());
            helper.assertBlockPresent(BCFactoryBlocks.TUBE.get(), WELL.below(2));
            helper.assertBlockPresent(Blocks.DIRT, WELL.below(3));
            helper.setBlock(WELL, Blocks.AIR);
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), WELL.below());
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), WELL.below(2));
        });
    }

    private static void assertCount(GameTestHelper helper, ChestBlockEntity chest, Item item, int expected) {
        int count = chest.countItem(item);
        helper.assertTrue(count == expected, "Expected " + expected + " " + item + " in the chest, found " + count);
    }
}
