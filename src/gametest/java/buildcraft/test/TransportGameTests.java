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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;

import buildcraft.core.BCCoreBlocks;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class TransportGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** A wooden pipe powered by a redstone engine pulls items out of a chest, and pipes carry them to another. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void woodPipeMovesItemsBetweenChests(GameTestHelper helper) {
        BlockPos source = new BlockPos(0, 1, 1);
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        BlockPos target = new BlockPos(2, 1, 2);

        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        ((ChestBlockEntity) helper.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_ITEM.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(wood.above(), BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(wood.above().north(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(target);
            helper.assertTrue(chest.getItem(0).getCount() == 4, "All 4 items should have arrived, found "
                + chest.getItem(0));
        });
    }

    /** Pipes of the same "separate" material don't connect to another separate material, but do to themselves. */
    @GameTest(template = EMPTY)
    public static void pipeConnections(GameTestHelper helper) {
        BlockPos stoneA = new BlockPos(0, 1, 1);
        BlockPos stoneB = new BlockPos(1, 1, 1);
        BlockPos cobble = new BlockPos(2, 1, 1);
        BlockPos gold = new BlockPos(1, 1, 2);
        placePipe(helper, stoneA, BCTransportItems.PIPE_STONE_ITEM.get());
        placePipe(helper, stoneB, BCTransportItems.PIPE_STONE_ITEM.get());
        placePipe(helper, cobble, BCTransportItems.PIPE_COBBLESTONE_ITEM.get());
        placePipe(helper, gold, BCTransportItems.PIPE_GOLD_ITEM.get());

        helper.runAfterDelay(2, () -> {
            Pipe middle = getPipe(helper, stoneB);
            helper.assertTrue(middle.isConnected(Direction.WEST), "Stone should connect to stone");
            helper.assertFalse(middle.isConnected(Direction.EAST), "Stone shouldn't connect to cobblestone");
            helper.assertTrue(middle.isConnected(Direction.SOUTH), "Stone should connect to gold");
            helper.succeed();
        });
    }

    /** Sandstone pipes only connect to other pipes. */
    @GameTest(template = EMPTY)
    public static void sandstoneIgnoresInventories(GameTestHelper helper) {
        BlockPos pipe = new BlockPos(1, 1, 1);
        placePipe(helper, pipe, BCTransportItems.PIPE_SANDSTONE_ITEM.get());
        helper.setBlock(pipe.west(), Blocks.CHEST);
        placePipe(helper, pipe.east(), BCTransportItems.PIPE_STONE_ITEM.get());

        helper.runAfterDelay(2, () -> {
            Pipe sandstone = getPipe(helper, pipe);
            helper.assertFalse(sandstone.isConnected(Direction.WEST), "Sandstone shouldn't connect to a chest");
            helper.assertTrue(sandstone.isConnected(Direction.EAST), "Sandstone should connect to pipes");
            helper.succeed();
        });
    }

    /** Items sent into a void pipe disappear. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void voidPipeDestroysItems(GameTestHelper helper) {
        BlockPos source = new BlockPos(0, 1, 1);
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos voidPipe = new BlockPos(2, 1, 1);

        helper.setBlock(source, Blocks.CHEST);
        ((ChestBlockEntity) helper.getBlockEntity(source)).setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_ITEM.get());
        placePipe(helper, voidPipe, BCTransportItems.PIPE_VOID_ITEM.get());
        helper.setBlock(wood.above(), BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(wood.above().north(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(source);
            helper.assertTrue(chest.getItem(0).isEmpty(), "The chest should have been emptied");
            helper.assertItemEntityNotPresent(Items.COBBLESTONE, wood, 3);
            helper.assertFalse(containsItems(getPipe(helper, voidPipe)), "The void pipe should be empty");
        });
    }

    /** A pipe, and the items inside it, survive being saved and loaded. */
    @GameTest(template = EMPTY)
    public static void pipeSavesItsDefinition(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        placePipe(helper, pos, BCTransportItems.PIPE_IRON_ITEM.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        CompoundTag saved = tile.saveWithoutMetadata();

        TilePipeHolder copy = new TilePipeHolder(helper.absolutePos(pos), tile.getBlockState());
        copy.load(saved);
        helper.assertTrue(copy.getPipe() != null, "The pipe should have been loaded");
        helper.assertTrue(
            copy.getPipe().getDefinition() == tile.getPipe().getDefinition(), "The pipe definition should match"
        );
        helper.succeed();
    }

    /** Places a pipe holder containing the given pipe, as if a player had placed it. */
    private static void placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
    }

    private static Pipe getPipe(GameTestHelper helper, BlockPos pos) {
        return ((TilePipeHolder) helper.getBlockEntity(pos)).getPipe();
    }

    private static boolean containsItems(Pipe pipe) {
        return pipe.getFlow() instanceof PipeFlowItems flow && flow.doesContainItems();
    }
}
