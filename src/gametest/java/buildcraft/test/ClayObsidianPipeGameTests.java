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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.behaviour.PipeBehaviourObsidian;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class ClayObsidianPipeGameTests {
    private static final String TEMPLATE = "gametest/empty5x5x5";
    private static final BlockPos PIPE = new BlockPos(1, 1, 2);

    /** Clay pipes send items into the inventories next to them rather than into other pipes. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void clayPipesPreferInventories(GameTestHelper helper) {
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        helper.setBlock(PIPE.north(), Blocks.CHEST);
        TilePipeHolder clay = placePipe(helper, PIPE, BCTransportItems.PIPE_CLAY_ITEM.get());
        placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(PIPE.east(2), Blocks.CHEST);
        helper.runAfterDelay(3, () -> {
            IInjectable injectable = clay.getCapability(PipeApi.CAP_INJECTABLE, Direction.WEST)
                .orElseThrow(() -> new GameTestAssertException("The pipe should accept items"));
            for (int i = 0; i < 4; i++) {
                injectable.injectItem(new ItemStack(Items.COBBLESTONE), true, Direction.WEST, null, 0.08);
            }
        });
        helper.succeedWhen(() -> {
            ChestBlockEntity north = (ChestBlockEntity) helper.getBlockEntity(PIPE.north());
            ChestBlockEntity east = (ChestBlockEntity) helper.getBlockEntity(PIPE.east(2));
            helper.assertTrue(north.countItem(Items.COBBLESTONE) == 4, "Every item should go into the chest");
            helper.assertTrue(east.countItem(Items.COBBLESTONE) == 0, "None should go through the other pipe");
        });
    }

    /** With power, obsidian pipes pick up the items in front of their open side. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void obsidianPipesPickUpItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 2);
        TilePipeHolder obsidian = placePipe(helper, pos, BCTransportItems.PIPE_OBSIDIAN_ITEM.get());
        helper.setBlock(pos.east(), Blocks.CHEST);
        Vec3 drop = helper.absoluteVec(Vec3.atBottomCenterOf(pos.west(2)));
        ItemEntity item = new ItemEntity(helper.getLevel(), drop.x, drop.y, drop.z, new ItemStack(Items.DIRT, 3));
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoGravity(true);
        helper.getLevel().addFreshEntity(item);
        helper.runAfterDelay(3, () -> {
            PipeBehaviourObsidian behaviour = (PipeBehaviourObsidian) obsidian.getPipe().getBehaviour();
            behaviour.receivePower(64 * MjAPI.MJ, false);
            helper.assertTrue(!item.isAlive(), "The item should be picked up");
        });
        helper.succeedWhen(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(pos.east());
            helper.assertTrue(chest.countItem(Items.DIRT) == 3, "The items should reach the chest");
        });
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }
}
