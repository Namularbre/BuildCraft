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
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockHeatExchange.EnumExchangePart;
import buildcraft.factory.tile.TileHeatExchange;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class HeatExchangeGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";
    // Facing north, the start is the block to the east (clockwise of north)
    private static final BlockPos START = new BlockPos(3, 1, 2);
    private static final BlockPos MIDDLE = new BlockPos(2, 1, 2);
    private static final BlockPos END = new BlockPos(1, 1, 2);

    /** 3 heat exchangers in a row become a start, a middle and an end. 2 in a row don't do anything. */
    @GameTest(template = EMPTY)
    public static void rowsBecomeHeatExchangers(GameTestHelper helper) {
        placeRow(helper, Direction.NORTH, END, MIDDLE, START);
        BlockPos loneA = new BlockPos(1, 1, 4);
        BlockPos loneB = new BlockPos(2, 1, 4);
        placeRow(helper, Direction.NORTH, loneA, loneB);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(START, BlockHeatExchange.PART, EnumExchangePart.START);
            helper.assertBlockProperty(MIDDLE, BlockHeatExchange.PART, EnumExchangePart.MIDDLE);
            helper.assertBlockProperty(END, BlockHeatExchange.PART, EnumExchangePart.END);
            helper.assertBlockProperty(loneA, BlockHeatExchange.PART, EnumExchangePart.MIDDLE);
            helper.assertBlockProperty(loneB, BlockHeatExchange.PART, EnumExchangePart.MIDDLE);
        });
    }

    /** Lava cooling down (and being used up) heats crude oil, which comes out of the top of the end. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void lavaHeatsOil(GameTestHelper helper) {
        placeRow(helper, Direction.NORTH, END, MIDDLE, START);
        FluidStack oil = new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 2_000);
        FluidStack lava = new FluidStack(Fluids.LAVA, 2_000);
        helper.runAfterDelay(2, () -> {
            // The fluid to heat goes in from below the start, the fluid to cool from the outer side of the end
            int oilIn = handler(helper, START, Direction.DOWN).fill(oil, FluidAction.EXECUTE);
            int lavaIn = handler(helper, END, Direction.WEST).fill(lava, FluidAction.EXECUTE);
            helper.assertTrue(oilIn == 2_000 && lavaIn == 2_000,
                "Should accept oil and lava: " + oilIn + ", " + lavaIn);
            helper.assertTrue(handler(helper, START, Direction.DOWN).fill(lava, FluidAction.SIMULATE) == 0,
                "Lava can't be heated");
        });
        helper.succeedWhen(() -> {
            FluidStack hot = handler(helper, END, Direction.UP).drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            helper.assertTrue(hot.getFluid().isSame(BCEnergyFluids.CRUDE_OIL.get(1).get()) && hot.getAmount() >= 100,
                "Hot oil should come out of the top of the end, got " + hot.getAmount() + " " + hot.getFluid());
            TileHeatExchange end = (TileHeatExchange) helper.getBlockEntity(END);
            int lavaLeft = end.getSection().tankInput.getFluidAmount();
            helper.assertTrue(lavaLeft == 2_000 - hot.getAmount(), "Lava should be used up as oil heats: " + lavaLeft);
        });
    }

    /** Turning a heat exchanger with a wrench turns the whole row around, and the tanks move with the start and
     * end. */
    @GameTest(template = EMPTY)
    public static void rotatingSwapsStartAndEnd(GameTestHelper helper) {
        placeRow(helper, Direction.NORTH, END, MIDDLE, START);
        helper.runAfterDelay(2, () -> {
            handler(helper, START, Direction.DOWN).fill(
                new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1_000), FluidAction.EXECUTE
            );
            helper.assertTrue(((TileHeatExchange) helper.getBlockEntity(MIDDLE)).rotate(), "Should rotate");
        });
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(START, BlockHeatExchange.FACING, Direction.SOUTH);
            helper.assertBlockProperty(END, BlockHeatExchange.PART, EnumExchangePart.START);
            helper.assertBlockProperty(START, BlockHeatExchange.PART, EnumExchangePart.END);
            TileHeatExchange newStart = (TileHeatExchange) helper.getBlockEntity(END);
            helper.assertTrue(newStart.getSection().tankInput.getFluidAmount() == 1_000, "The oil should move along");
        });
    }

    private static void placeRow(GameTestHelper helper, Direction facing, BlockPos... positions) {
        for (BlockPos pos : positions) {
            helper.setBlock(pos, BCFactoryBlocks.HEAT_EXCHANGE.get().defaultBlockState()
                .setValue(BlockHeatExchange.FACING, facing));
        }
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER, side)
            .orElseThrow(() -> new GameTestAssertException("No fluid handler at " + pos + " on " + side));
    }
}
