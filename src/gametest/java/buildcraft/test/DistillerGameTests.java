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
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager.IDistillationRecipe;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileDistiller;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class DistillerGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos DISTILLER = new BlockPos(1, 1, 1);

    /** Crude oil goes in through a side, and comes out as gaseous fuel (top) and heavy oil (bottom). */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void distillsCrudeOil(GameTestHelper helper) {
        TileDistiller distiller = place(helper);
        FluidStack oil = new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1_000);
        IDistillationRecipe recipe = BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry()
            .getRecipeForInput(oil);
        helper.assertTrue(recipe != null, "Crude oil should have a distillation recipe");
        helper.assertTrue(recipe.outGas().getFluid().isSame(BCEnergyFluids.FUEL_GASEOUS.get(0).get()), "Gas out");
        helper.assertTrue(recipe.outLiquid().getFluid().isSame(BCEnergyFluids.OIL_HEAVY.get(0).get()), "Liquid out");

        int filled = handler(distiller, Direction.NORTH).fill(oil, FluidAction.EXECUTE);
        helper.assertTrue(filled == 1_000, "The side should accept crude oil, accepted " + filled);
        IMjReceiver receiver = distiller.getCapability(MjAPI.CAP_RECEIVER, Direction.NORTH)
            .orElseThrow(IllegalStateException::new);
        helper.onEachTick(() -> receiver.receivePower(6 * MjAPI.MJ, false));

        helper.succeedWhen(() -> {
            FluidStack gas = handler(distiller, Direction.UP).drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            FluidStack liquid = handler(distiller, Direction.DOWN).drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            int operations = gas.getAmount() / recipe.outGas().getAmount();
            helper.assertTrue(operations >= 5, "The distiller should have distilled at least 5 times, did "
                + operations);
            helper.assertTrue(liquid.getAmount() == operations * recipe.outLiquid().getAmount(),
                "Heavy oil should come out with the gas, got " + liquid.getAmount());
            int left = distiller.tankIn.getFluidAmount();
            helper.assertTrue(left == 1_000 - operations * recipe.in().getAmount(), "Crude oil used: " + left);
        });
    }

    /** Only fluids that can be distilled go in, only through the sides; outputs can't be filled. */
    @GameTest(template = EMPTY)
    public static void onlyDistillableFluidsGoIn(GameTestHelper helper) {
        TileDistiller distiller = place(helper);
        FluidStack water = new FluidStack(Fluids.WATER, 1_000);
        FluidStack oil = new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1_000);
        helper.assertTrue(handler(distiller, Direction.EAST).fill(water, FluidAction.EXECUTE) == 0, "No water");
        helper.assertTrue(handler(distiller, Direction.UP).fill(oil, FluidAction.EXECUTE) == 0, "Not from the top");
        helper.assertTrue(handler(distiller, Direction.DOWN).fill(oil, FluidAction.EXECUTE) == 0, "Not from below");
        helper.assertTrue(handler(distiller, Direction.EAST).drain(1_000, FluidAction.EXECUTE).isEmpty(),
            "The input can't be drained");
        helper.succeed();
    }

    /** Without power, nothing is distilled. */
    @GameTest(template = EMPTY)
    public static void needsPower(GameTestHelper helper) {
        TileDistiller distiller = place(helper);
        handler(distiller, Direction.SOUTH).fill(
            new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1_000), FluidAction.EXECUTE
        );
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(distiller.tankIn.getFluidAmount() == 1_000, "Nothing should be distilled");
            helper.assertTrue(distiller.tankGasOut.isEmpty(), "No gas should come out");
            helper.succeed();
        });
    }

    private static TileDistiller place(GameTestHelper helper) {
        helper.setBlock(DISTILLER, BCFactoryBlocks.DISTILLER.get());
        return (TileDistiller) helper.getBlockEntity(DISTILLER);
    }

    private static IFluidHandler handler(TileDistiller distiller, Direction side) {
        return distiller.getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
    }
}
