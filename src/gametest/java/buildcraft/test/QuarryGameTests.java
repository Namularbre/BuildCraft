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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.tile.TileQuarry;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class QuarryGameTests {
    private static final String BIG = "gametest/empty7x7x7";
    private static final BlockPos QUARRY = new BlockPos(0, 1, 3);
    /** The frame goes from (1, 1, 1) to (5, 5, 5): it is made 4 blocks tall. */
    private static final BlockPos FRAME_MIN = new BlockPos(1, 1, 1);
    private static final BlockPos FRAME_MAX = new BlockPos(5, 1, 5);

    /** A powered quarry builds its frame, then mines the area inside it, sending what it mines to the chest on top
     * of it. */
    @GameTest(template = BIG, timeoutTicks = 600)
    public static void quarryBuildsFrameAndMines(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
            }
        }
        // Something in the way of the frame, which the quarry clears
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE);
        TileQuarry quarry = placeQuarry(helper);
        helper.setBlock(QUARRY.above(), Blocks.CHEST);
        IMjReceiver receiver = quarry.getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The quarry should accept power"));
        helper.onEachTick(() -> receiver.receivePower(1000 * MjAPI.MJ, false));

        helper.succeedWhen(() -> {
            for (BlockPos corner : new BlockPos[] {
                FRAME_MIN, new BlockPos(5, 1, 5), new BlockPos(1, 5, 1), new BlockPos(5, 5, 5), new BlockPos(5, 3, 5)
            }) {
                helper.assertBlockPresent(BCBuildersBlocks.FRAME.get(), corner);
            }
            int dirt = ((ChestBlockEntity) helper.getBlockEntity(QUARRY.above())).countItem(Items.DIRT);
            helper.assertTrue(dirt >= 9, "The dirt inside the frame should be mined into the chest, got " + dirt);
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 0, z));
                }
            }
        });
    }

    /** Breaking the quarry removes its frame. */
    @GameTest(template = BIG, timeoutTicks = 200)
    public static void frameGoesWithTheQuarry(GameTestHelper helper) {
        TileQuarry quarry = placeQuarry(helper);
        IMjReceiver receiver = quarry.getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The quarry should accept power"));
        helper.onEachTick(() -> receiver.receivePower(1000 * MjAPI.MJ, false));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCBuildersBlocks.FRAME.get(), new BlockPos(5, 5, 5));
            helper.setBlock(QUARRY, Blocks.AIR);
            helper.assertBlockNotPresent(BCBuildersBlocks.FRAME.get(), new BlockPos(5, 5, 5));
            helper.assertBlockNotPresent(BCBuildersBlocks.FRAME.get(), FRAME_MIN);
        });
    }

    private static TileQuarry placeQuarry(GameTestHelper helper) {
        // Facing west, so that its area is to the east
        helper.setBlock(QUARRY, BCBuildersBlocks.QUARRY.get().defaultBlockState()
            .setValue(BlockQuarry.FACING, Direction.WEST));
        TileQuarry quarry = (TileQuarry) helper.getBlockEntity(QUARRY);
        quarry.setArea(helper.absolutePos(FRAME_MIN), helper.absolutePos(FRAME_MAX));
        return quarry;
    }
}
