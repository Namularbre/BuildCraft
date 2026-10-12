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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.tile.TileAutoWorkbenchItems;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class AutoWorkbenchGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    /** The slot of the external inventory that holds the result. */
    private static final int RESULT = 9;

    /** A powered workbench crafts its blueprint from the materials piped in, and only accepts those materials. */
    @GameTest(template = EMPTY)
    public static void craftsTheBlueprint(GameTestHelper helper) {
        TileAutoWorkbenchItems bench = place(helper);
        bench.invBlueprint.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        powerFully(helper, bench);
        helper.runAfterDelay(2, () -> {
            IItemHandler inv = inventory(bench);
            ItemStack left = ItemHandlerHelper.insertItemStacked(inv, new ItemStack(Items.OAK_LOG, 2), false);
            helper.assertTrue(left.isEmpty(), "The logs should go in");
            left = ItemHandlerHelper.insertItemStacked(inv, new ItemStack(Items.COBBLESTONE), false);
            helper.assertTrue(!left.isEmpty(), "Cobblestone isn't part of the recipe, so it shouldn't go in");
        });
        helper.succeedWhen(() -> {
            ItemStack result = inventory(bench).extractItem(RESULT, 64, true);
            helper.assertTrue(result.is(Items.OAK_PLANKS) && result.getCount() == 8,
                "2 logs should make 8 planks, got " + result);
        });
    }

    /** What a recipe leaves behind (an empty bottle from honey) goes to the chest next to the workbench. */
    @GameTest(template = EMPTY)
    public static void leftoversGoToNeighbours(GameTestHelper helper) {
        TileAutoWorkbenchItems bench = place(helper);
        helper.setBlock(BENCH.east(), Blocks.CHEST);
        bench.invBlueprint.setStackInSlot(0, new ItemStack(Items.HONEY_BOTTLE));
        powerFully(helper, bench);
        helper.runAfterDelay(2,
            () -> ItemHandlerHelper.insertItemStacked(inventory(bench), new ItemStack(Items.HONEY_BOTTLE), false));
        helper.succeedWhen(() -> {
            ItemStack result = inventory(bench).extractItem(RESULT, 64, true);
            helper.assertTrue(result.is(Items.SUGAR) && result.getCount() == 3, "Honey should make 3 sugar");
            int bottles = ((ChestBlockEntity) helper.getBlockEntity(BENCH.east())).countItem(Items.GLASS_BOTTLE);
            helper.assertTrue(bottles == 1, "The empty bottle should go in the chest, found " + bottles);
        });
    }

    /** Clicking a blueprint slot with an item sets a copy of it, without taking the item. */
    @GameTest(template = EMPTY)
    public static void blueprintSlotsCopyItems(GameTestHelper helper) {
        TileAutoWorkbenchItems bench = place(helper);
        Player player = helper.makeMockPlayer();
        ContainerAutoCraftItems container = new ContainerAutoCraftItems(1, player, bench);
        container.setCarried(new ItemStack(Items.STICK, 5));
        // Slot 0 is the result, then come the 9 blueprint slots
        container.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(bench.invBlueprint.getStackInSlot(0).is(Items.STICK)
            && bench.invBlueprint.getStackInSlot(0).getCount() == 1, "The blueprint should show one stick");
        helper.assertTrue(container.getCarried().getCount() == 5, "The player should keep their sticks");

        container.setCarried(ItemStack.EMPTY);
        container.clicked(1, 0, ClickType.PICKUP, player);
        helper.assertTrue(bench.invBlueprint.getStackInSlot(0).isEmpty(), "Clicking with nothing clears the slot");
        helper.succeed();
    }

    private static TileAutoWorkbenchItems place(GameTestHelper helper) {
        helper.setBlock(BENCH, BCFactoryBlocks.AUTO_WORKBENCH_ITEMS.get());
        return (TileAutoWorkbenchItems) helper.getBlockEntity(BENCH);
    }

    private static void powerFully(GameTestHelper helper, TileAutoWorkbenchItems bench) {
        IMjReceiver receiver = bench.getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The workbench should accept power"));
        helper.onEachTick(() -> receiver.receivePower(TileAutoWorkbenchItems.POWER_REQUIRED, false));
    }

    private static IItemHandler inventory(TileAutoWorkbenchItems bench) {
        return bench.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The workbench should have an inventory"));
    }
}
