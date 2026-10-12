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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockChute;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class ChuteGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";
    private static final BlockPos CHUTE = new BlockPos(2, 1, 2);

    /** A chute facing up picks up the items lying on it without power, and sends them to the chest below. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void pickupFacingUpIsFree(GameTestHelper helper) {
        placeChute(helper, Direction.UP);
        helper.setBlock(CHUTE.below(), Blocks.CHEST);
        Vec3 onTop = helper.absoluteVec(Vec3.atCenterOf(CHUTE).add(0, 0.6, 0));
        ItemEntity item = new ItemEntity(helper.getLevel(), onTop.x, onTop.y, onTop.z,
            new ItemStack(Items.COBBLESTONE, 5), 0, 0, 0);
        helper.getLevel().addFreshEntity(item);

        helper.succeedWhen(() -> {
            int count = ((ChestBlockEntity) helper.getBlockEntity(CHUTE.below())).countItem(Items.COBBLESTONE);
            helper.assertTrue(count == 5, "The chest should get the 5 items, has " + count);
            helper.assertEntityNotPresent(EntityType.ITEM);
            helper.assertBlockProperty(CHUTE, BlockChute.CONNECTED.get(Direction.DOWN), true);
            helper.assertBlockProperty(CHUTE, BlockChute.CONNECTED.get(Direction.UP), false);
        });
    }

    /** Items put in a chute go to the inventories around it, but never out of its opening, and other blocks can't
     * take them out. */
    @GameTest(template = EMPTY)
    public static void itemsLeaveThroughTheSides(GameTestHelper helper) {
        placeChute(helper, Direction.NORTH);
        BlockPos front = CHUTE.north();
        BlockPos back = CHUTE.south();
        helper.setBlock(front, Blocks.CHEST);
        helper.setBlock(back, Blocks.CHEST);
        IItemHandler chute = helper.getBlockEntity(CHUTE).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The chute should have an inventory"));
        ItemStack left = ItemHandlerHelper.insertItemStacked(chute, new ItemStack(Items.DIRT, 4), false);
        helper.assertTrue(left.isEmpty(), "The chute should accept 4 items");
        helper.assertTrue(chute.extractItem(0, 1, false).isEmpty(), "Items can't be taken out of a chute");

        helper.succeedWhen(() -> {
            int inBack = ((ChestBlockEntity) helper.getBlockEntity(back)).countItem(Items.DIRT);
            int inFront = ((ChestBlockEntity) helper.getBlockEntity(front)).countItem(Items.DIRT);
            helper.assertTrue(inBack == 4, "The chest behind should get the items, has " + inBack);
            helper.assertTrue(inFront == 0, "Nothing should go out of the opening, got " + inFront);
        });
    }

    private static void placeChute(GameTestHelper helper, Direction facing) {
        helper.setBlock(CHUTE, BCFactoryBlocks.CHUTE.get().defaultBlockState().setValue(BlockChute.FACING, facing));
    }
}
