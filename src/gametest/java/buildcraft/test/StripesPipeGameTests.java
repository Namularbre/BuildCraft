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

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;
import buildcraft.transport.tile.TilePipeHolder;

/** A chest, a stone pipe, and a stripes pipe facing east (away from the stone pipe). */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class StripesPipeGameTests {
    private static final String TEMPLATE = "gametest/empty5x5x5";
    private static final BlockPos CHEST = new BlockPos(0, 1, 2);
    private static final BlockPos STONE = new BlockPos(1, 1, 2);
    private static final BlockPos STRIPES = new BlockPos(2, 1, 2);
    private static final BlockPos FRONT = STRIPES.east();

    /** With power, stripes pipes break the block they face, and send what it drops back. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void stripesPipesBreakBlocks(GameTestHelper helper) {
        TilePipeHolder stripes = setUp(helper);
        helper.setBlock(FRONT, Blocks.DIRT);
        helper.onEachTick(() -> {
            if (stripes.getPipe() != null && stripes.getPipe().getBehaviour() instanceof PipeBehaviourStripes b) {
                b.receivePower(16 * MjAPI.MJ, false);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, FRONT);
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(CHEST);
            helper.assertTrue(chest.countItem(Items.DIRT) == 1, "The dirt should be sent back to the chest");
        });
    }

    /** Blocks that reach the end of the pipe are placed in front of it. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void stripesPipesPlaceBlocks(GameTestHelper helper) {
        setUp(helper);
        helper.runAfterDelay(3, () -> inject(helper, new ItemStack(Items.COBBLESTONE)));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.COBBLESTONE, FRONT));
    }

    /** An item pipe moves the stripes pipe forwards (and takes its place), and a void pipe moves it back. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void stripesPipesExtendAndRetract(GameTestHelper helper) {
        setUp(helper);
        helper.runAfterDelay(3, () -> inject(helper, new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get())));
        boolean[] retracting = { false };
        helper.onEachTick(() -> {
            if (!retracting[0] && helper.getBlockEntity(FRONT) instanceof TilePipeHolder moved
                && moved.getPipe() != null && moved.getPipe().getBehaviour() instanceof PipeBehaviourStripes) {
                retracting[0] = true;
                TilePipeHolder placed = (TilePipeHolder) helper.getBlockEntity(STRIPES);
                helper.assertTrue(placed.getPipe().getDefinition() == BCTransportItems.PIPE_STONE_ITEM.get()
                    .getDefinition(), "A stone pipe should be where the stripes pipe was");
                inject(helper, new ItemStack(BCTransportItems.PIPE_VOID_ITEM.get()));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(retracting[0], "The pipe should have moved forwards");
            helper.assertBlockPresent(Blocks.AIR, FRONT);
            TilePipeHolder back = (TilePipeHolder) helper.getBlockEntity(STRIPES);
            helper.assertTrue(back.getPipe().getBehaviour() instanceof PipeBehaviourStripes,
                "The stripes pipe should be back");
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(CHEST);
            helper.assertTrue(chest.countItem(BCTransportItems.PIPE_STONE_ITEM.get()) == 1,
                "The stone pipe should be sent back");
            helper.assertTrue(chest.countItem(BCTransportItems.PIPE_VOID_ITEM.get()) == 1,
                "The void pipe should be sent back");
        });
    }

    private static void inject(GameTestHelper helper, ItemStack stack) {
        TilePipeHolder stone = (TilePipeHolder) helper.getBlockEntity(STONE);
        IInjectable injectable = stone.getCapability(PipeApi.CAP_INJECTABLE, Direction.WEST)
            .orElseThrow(() -> new GameTestAssertException("The pipe should accept items"));
        injectable.injectItem(stack, true, Direction.WEST, null, 0.08);
    }

    private static TilePipeHolder setUp(GameTestHelper helper) {
        helper.setBlock(CHEST, Blocks.CHEST);
        placePipe(helper, STONE, BCTransportItems.PIPE_STONE_ITEM.get());
        return placePipe(helper, STRIPES, BCTransportItems.PIPE_STRIPES_ITEM.get());
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }
}
