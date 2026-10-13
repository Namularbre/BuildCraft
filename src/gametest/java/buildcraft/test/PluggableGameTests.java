/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportPlugs;
import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class PluggableGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos PIPE = new BlockPos(1, 1, 1);

    /** A plug on a side of a pipe stops it from connecting there. */
    @GameTest(template = EMPTY)
    public static void plugsBlockConnections(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_STONE_ITEM.get());
        TilePipeHolder other = placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(pipe.getPipe().isConnected(Direction.EAST), "The pipes should connect at first");
            pipe.replacePluggable(Direction.EAST, BCTransportPlugs.BLOCKER.creator.createSimplePluggable(
                BCTransportPlugs.BLOCKER, pipe, Direction.EAST
            ));
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!pipe.getPipe().isConnected(Direction.EAST), "The plugged pipe shouldn't connect");
            helper.assertTrue(!other.getPipe().isConnected(Direction.WEST), "Nor should the other pipe");
            helper.succeed();
        });
    }

    /** Using a plug on a pipe puts it on the side that was clicked. The plug is saved, and dropped with the pipe. */
    @GameTest(template = EMPTY)
    public static void plugsArePlacedSavedAndDropped(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_STONE_ITEM.get());
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BCTransportItems.PLUG_BLOCKER.get(), 2));
        BlockPos absolute = helper.absolutePos(PIPE);
        // The top of the centre of the pipe
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.25, 0), Direction.UP, absolute,
            false);
        helper.getLevel().getBlockState(absolute).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(pipe.getPluggable(Direction.UP) instanceof PluggableBlocker, "A plug should be on top");

        CompoundTag saved = pipe.saveWithoutMetadata();
        pipe.replacePluggable(Direction.UP, null);
        pipe.load(saved);
        helper.assertTrue(pipe.getPluggable(Direction.UP) instanceof PluggableBlocker, "The plug should be saved");

        NonNullList<ItemStack> drops = NonNullList.create();
        pipe.addDrops(drops);
        helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(BCTransportItems.PLUG_BLOCKER.get())),
            "The plug should drop with the pipe, dropped " + drops);
        helper.succeed();
    }

    /** Structure pipes connect to each other, but not to anything else. */
    @GameTest(template = EMPTY)
    public static void structurePipesOnlyConnectToEachOther(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_STRUCTURE.get());
        placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STRUCTURE.get());
        placePipe(helper, PIPE.north(), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        helper.succeedWhen(() -> {
            helper.assertTrue(pipe.getPipe().isConnected(Direction.EAST), "Structure pipes should connect together");
            helper.assertTrue(!pipe.getPipe().isConnected(Direction.NORTH), "Not to other pipes");
            helper.assertTrue(!pipe.getPipe().isConnected(Direction.WEST), "Not to inventories");
        });
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }
}
