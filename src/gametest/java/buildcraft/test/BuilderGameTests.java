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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.block.BlockBuilder;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotStorage;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;

/** The architect table scans a box (2 by 2 blocks, at y = 1), and a builder builds it again further south. Both face
 * north, so the copy is moved by the distance between them. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class BuilderGameTests {
    private static final String TEMPLATE = "gametest/empty5x5x5";
    private static final BlockPos ARCHITECT = new BlockPos(0, 1, 0);
    private static final BlockPos BOX_MIN = new BlockPos(2, 1, 0), BOX_MAX = new BlockPos(3, 1, 1);
    private static final BlockPos BUILDER = new BlockPos(0, 1, 3);
    /** How far the copy is from the original. */
    private static final BlockPos SHIFT = BUILDER.subtract(ARCHITECT);

    /** Blueprints keep every block, turned the right way. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void blueprintsAreScannedAndBuilt(GameTestHelper helper) {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        helper.setBlock(BOX_MIN, Blocks.STONE);
        helper.setBlock(BOX_MAX, stairs);
        ItemStack scanned = scan(helper, new ItemStack(BCBuildersBlocks.BLUEPRINT.get()));
        ItemSnapshot.Header header = ItemSnapshot.getHeader(scanned);
        Snapshot snapshot = SnapshotStorage.get(helper.getLevel().getServer()).get(header.key());
        helper.assertTrue(snapshot instanceof Blueprint, "The blueprint should be stored, found " + snapshot);
        helper.assertTrue(((Blueprint) snapshot).palette.contains(stairs), "The stairs should be in the blueprint");

        TileBuilder builder = placeBuilder(helper, scanned);
        builder.invResources.insertItem(0, new ItemStack(Items.STONE, 4), false);
        builder.invResources.insertItem(1, new ItemStack(Items.OAK_STAIRS, 4), false);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.STONE, BOX_MIN.offset(SHIFT));
            helper.assertTrue(helper.getBlockState(BOX_MAX.offset(SHIFT)) == stairs, "The stairs should face east");
            helper.assertBlockPresent(Blocks.AIR, BOX_MIN.offset(SHIFT).east().south().south());
            helper.assertTrue(builder.invResources.getStackInSlot(0).getCount() == 3, "1 stone is used");
        });
    }

    /** Templates only keep where blocks are: any block is used to build them. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void templatesAreBuiltWithAnyBlock(GameTestHelper helper) {
        helper.setBlock(BOX_MIN, Blocks.STONE);
        helper.setBlock(BOX_MAX, Blocks.DIRT);
        ItemStack scanned = scan(helper, new ItemStack(BCBuildersBlocks.TEMPLATE.get()));
        TileBuilder builder = placeBuilder(helper, scanned);
        builder.invResources.insertItem(0, new ItemStack(Items.COBBLESTONE, 8), false);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.COBBLESTONE, BOX_MIN.offset(SHIFT));
            helper.assertBlockPresent(Blocks.COBBLESTONE, BOX_MAX.offset(SHIFT));
            helper.assertBlockPresent(Blocks.AIR, BOX_MIN.offset(SHIFT).south());
        });
    }

    /** Scans the box straight away (the scan itself is done over a few ticks in game). */
    private static ItemStack scan(GameTestHelper helper, ItemStack blank) {
        helper.setBlock(ARCHITECT, BCBuildersBlocks.ARCHITECT.get().defaultBlockState()
            .setValue(BlockArchitectTable.FACING, Direction.NORTH));
        TileArchitectTable table = (TileArchitectTable) helper.getBlockEntity(ARCHITECT);
        table.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCBuildersBlocks.ARCHITECT.get()));
        table.setBox(new Box(helper.absolutePos(BOX_MIN), helper.absolutePos(BOX_MAX)));
        table.name = "Test";
        table.invIn.insertItem(0, blank, false);
        for (int i = 0; i < 5 && table.invOut.getStackInSlot(0).isEmpty(); i++) {
            table.tick();
        }
        ItemStack scanned = table.invOut.getStackInSlot(0);
        ItemSnapshot.Header header = ItemSnapshot.getHeader(scanned);
        helper.assertTrue(header != null && header.name().equals("Test"), "The scan should be named, got " + header);
        helper.assertTrue(table.invIn.getStackInSlot(0).isEmpty(), "The blank should be used up");
        return scanned;
    }

    private static TileBuilder placeBuilder(GameTestHelper helper, ItemStack snapshot) {
        helper.setBlock(BUILDER, BCBuildersBlocks.BUILDER.get().defaultBlockState()
            .setValue(BlockBuilder.FACING, Direction.NORTH));
        TileBuilder builder = (TileBuilder) helper.getBlockEntity(BUILDER);
        builder.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCBuildersBlocks.BUILDER.get()));
        builder.getBattery().addPower(16_000 * MjAPI.MJ, false);
        builder.invSnapshot.insertItem(0, snapshot, false);
        return builder;
    }
}
