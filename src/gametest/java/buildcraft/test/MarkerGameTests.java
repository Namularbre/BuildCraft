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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.tile.TileMarkerVolume;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class MarkerGameTests {
    private static final String BIG = "gametest/empty7x7x7";
    private static final BlockPos CORNER = new BlockPos(2, 1, 1);
    private static final BlockPos ALONG_X = new BlockPos(6, 1, 1);
    private static final BlockPos ALONG_Z = new BlockPos(2, 1, 5);

    /** Clicking a marker connects it to the markers in line with it, which mark out a box. Removing one of them
     * breaks the box apart once fewer than 2 are left. */
    @GameTest(template = BIG)
    public static void markersConnect(GameTestHelper helper) {
        placeMarkers(helper);
        TileMarkerVolume corner = marker(helper, CORNER);
        corner.tryConnect();

        Box box = corner.getBox();
        helper.assertTrue(box != null, "The markers should connect");
        helper.assertTrue(box.min().equals(helper.absolutePos(CORNER))
            && box.max().equals(helper.absolutePos(new BlockPos(6, 1, 5))), "Wrong box: " + box);
        helper.assertTrue(marker(helper, ALONG_X).getConnection().size() == 3, "Every marker should know the box");

        helper.setBlock(ALONG_Z, Blocks.AIR);
        helper.assertTrue(marker(helper, CORNER).getConnection().size() == 2, "Two markers should stay connected");
        helper.setBlock(ALONG_X, Blocks.AIR);
        helper.assertTrue(marker(helper, CORNER).getBox() == null, "A single marker isn't a box");
        helper.succeed();
    }

    /** A quarry placed right next to a box of markers takes it as its area, and the markers go. */
    @GameTest(template = BIG)
    public static void quarryUsesMarkers(GameTestHelper helper) {
        placeMarkers(helper);
        marker(helper, CORNER).tryConnect();
        BlockPos quarryPos = CORNER.west();
        helper.setBlock(quarryPos, BCBuildersBlocks.QUARRY.get().defaultBlockState()
            .setValue(BlockQuarry.FACING, Direction.WEST));
        TileQuarry quarry = (TileQuarry) helper.getBlockEntity(quarryPos);
        quarry.onPlacedBy(null, ItemStack.EMPTY);

        Box frame = quarry.getFrameBox();
        helper.assertTrue(frame != null && frame.min().equals(helper.absolutePos(CORNER))
            && frame.max().equals(helper.absolutePos(new BlockPos(6, 5, 5))),
            "The frame should follow the markers: " + frame);
        helper.assertBlockNotPresent(BCCoreBlocks.MARKER_VOLUME.get(), CORNER);
        helper.assertBlockNotPresent(BCCoreBlocks.MARKER_VOLUME.get(), ALONG_X);
        helper.succeed();
    }

    private static void placeMarkers(GameTestHelper helper) {
        for (BlockPos pos : new BlockPos[] { CORNER, ALONG_X, ALONG_Z }) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, BCCoreBlocks.MARKER_VOLUME.get());
        }
    }

    private static TileMarkerVolume marker(GameTestHelper helper, BlockPos pos) {
        return (TileMarkerVolume) helper.getBlockEntity(pos);
    }
}
