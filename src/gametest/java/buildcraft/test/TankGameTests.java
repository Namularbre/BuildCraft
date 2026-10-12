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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.tile.TileTank;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class TankGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos BOTTOM = new BlockPos(1, 0, 1);

    /** Stacked tanks act as one: liquids fill from the bottom and drain from the top. */
    @GameTest(template = EMPTY)
    public static void stackedTanksFillFromTheBottom(GameTestHelper helper) {
        placeColumn(helper);
        TileTank top = tank(helper, BOTTOM.above(2));
        int filled = top.getColumnHandler().fill(new FluidStack(Fluids.WATER, 20_000), FluidAction.EXECUTE);
        helper.assertTrue(filled == 20_000, "The column should accept 20 buckets, accepted " + filled);
        helper.assertTrue(tank(helper, BOTTOM).tank.getFluidAmount() == TileTank.CAPACITY, "Bottom tank full");
        helper.assertTrue(tank(helper, BOTTOM.above()).tank.getFluidAmount() == 4_000, "Middle tank has the rest");
        helper.assertTrue(top.tank.isEmpty(), "Top tank empty");

        FluidStack drained = tank(helper, BOTTOM).getColumnHandler().drain(5_000, FluidAction.EXECUTE);
        helper.assertTrue(drained.getAmount() == 5_000, "Should drain 5 buckets");
        helper.assertTrue(tank(helper, BOTTOM.above()).tank.isEmpty(), "Draining starts from the top");
        helper.assertTrue(tank(helper, BOTTOM).tank.getFluidAmount() == 15_000, "Then the bottom");
        helper.succeed();
    }

    /** Gases fill tanks from the top. */
    @GameTest(template = EMPTY)
    public static void gasesFillFromTheTop(GameTestHelper helper) {
        placeColumn(helper);
        FluidStack gas = new FluidStack(BCEnergyFluids.FUEL_GASEOUS.get(0).get(), 1_000);
        tank(helper, BOTTOM).getColumnHandler().fill(gas, FluidAction.EXECUTE);
        helper.assertTrue(tank(helper, BOTTOM.above(2)).tank.getFluidAmount() == 1_000, "Gas should go to the top");
        helper.assertTrue(tank(helper, BOTTOM).tank.isEmpty(), "Bottom tank empty");
        helper.succeed();
    }

    /** A column only holds one fluid at a time. */
    @GameTest(template = EMPTY)
    public static void tanksHoldOneFluid(GameTestHelper helper) {
        placeColumn(helper);
        tank(helper, BOTTOM).getColumnHandler().fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE);
        int lava = tank(helper, BOTTOM.above(2)).getColumnHandler().fill(
            new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE
        );
        helper.assertTrue(lava == 0, "Lava shouldn't go in a column of water");
        helper.succeed();
    }

    /** Buckets empty into tanks, the side texture joins the tank below, and comparators read the level. */
    @GameTest(template = EMPTY)
    public static void bucketsAndComparators(GameTestHelper helper) {
        placeColumn(helper);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        BlockPos absolute = helper.absolutePos(BOTTOM);
        tank(helper, BOTTOM).onActivated(
            player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute,
                false)
        );
        helper.assertTrue(tank(helper, BOTTOM).tank.getFluidAmount() == 1_000, "The bucket should empty in the tank");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "The player should get the empty bucket back");

        helper.assertBlockProperty(BOTTOM, BlockTank.JOINED_BELOW, false);
        helper.assertBlockProperty(BOTTOM.above(), BlockTank.JOINED_BELOW, true);

        int comparator = tank(helper, BOTTOM).getComparatorLevel();
        helper.assertTrue(comparator == 1, "1 bucket out of 16 should give a comparator level of 1, got " + comparator);
        helper.succeed();
    }

    /** Places 3 tanks from the top down, so that each tank placed below another updates its joined texture (setBlock
     * doesn't go through the placement logic a player would). */
    private static void placeColumn(GameTestHelper helper) {
        for (int y = 2; y >= 0; y--) {
            helper.setBlock(BOTTOM.above(y), BCFactoryBlocks.TANK.get());
        }
    }

    private static TileTank tank(GameTestHelper helper, BlockPos pos) {
        return (TileTank) helper.getBlockEntity(pos);
    }
}
