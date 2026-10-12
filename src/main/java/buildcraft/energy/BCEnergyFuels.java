/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager;

import buildcraft.lib.fluid.BCFluid;

/** The fuels and coolants of the combustion engine, and the refining recipes of the distiller and the heat exchanger,
 * with the values of 1.12. Only the cool variant of each fluid is a fuel. */
public final class BCEnergyFuels {
    /** Ticks that a bucket of fuel burns for is derived from this (a multiple of 3, 5, 16 and 1000). */
    private static final int TIME_BASE = 240_000;

    // How much of each product refining crude oil gives (relative amounts)
    private static final int OIL = 8;
    private static final int GAS = 16;
    private static final int LIGHT = 4;
    private static final int DENSE = 2;
    private static final int RESIDUE = 1;
    private static final int GAS_LIGHT = 10;
    private static final int LIGHT_DENSE = 5;
    private static final int DENSE_RESIDUE = 2;
    private static final int LIGHT_DENSE_RESIDUE = 3;
    private static final int GAS_LIGHT_DENSE = 8;

    private BCEnergyFuels() {}

    /** Must be called once the fluids are registered. */
    public static void register() {
        BuildcraftFuelRegistry.coolant.addCoolant(Fluids.WATER, 0.0023f);
        BuildcraftFuelRegistry.coolant.addSolidCoolant(
            new ItemStack(Items.ICE), new FluidStack(Fluids.WATER, 1000), 1.5f
        );
        BuildcraftFuelRegistry.coolant.addSolidCoolant(
            new ItemStack(Items.PACKED_ICE), new FluidStack(Fluids.WATER, 1000), 2f
        );

        addFuel(BCEnergyFluids.FUEL_GASEOUS, GAS, 8, 4);
        addFuel(BCEnergyFluids.FUEL_LIGHT, LIGHT, 6, 6);
        addFuel(BCEnergyFluids.FUEL_DENSE, DENSE, 4, 12);

        addFuel(BCEnergyFluids.FUEL_MIXED_LIGHT, GAS_LIGHT, 3, 5);
        addFuel(BCEnergyFluids.FUEL_MIXED_HEAVY, LIGHT_DENSE, 5, 8);
        addDirtyFuel(BCEnergyFluids.OIL_DENSE, DENSE_RESIDUE, 4, 4);

        addFuel(BCEnergyFluids.OIL_DISTILLED, GAS_LIGHT_DENSE, 1, 5);
        addDirtyFuel(BCEnergyFluids.OIL_HEAVY, LIGHT_DENSE_RESIDUE, 2, 4);

        addDirtyFuel(BCEnergyFluids.CRUDE_OIL, OIL, 3, 4);

        registerRefining();
    }

    private static void registerRefining() {
        List<BCFluid> gasLightDenseResidue = BCEnergyFluids.CRUDE_OIL;
        List<BCFluid> gasLightDense = BCEnergyFluids.OIL_DISTILLED;
        List<BCFluid> gasLight = BCEnergyFluids.FUEL_MIXED_LIGHT;
        List<BCFluid> gas = BCEnergyFluids.FUEL_GASEOUS;
        List<BCFluid> lightDenseResidue = BCEnergyFluids.OIL_HEAVY;
        List<BCFluid> lightDense = BCEnergyFluids.FUEL_MIXED_HEAVY;
        List<BCFluid> light = BCEnergyFluids.FUEL_LIGHT;
        List<BCFluid> denseResidue = BCEnergyFluids.OIL_DENSE;
        List<BCFluid> dense = BCEnergyFluids.FUEL_DENSE;
        List<BCFluid> residue = BCEnergyFluids.OIL_RESIDUE;

        // Each fluid can be split at the heat that separates the gas part from the liquid part
        addDistillation(gasLightDenseResidue, OIL, gas, GAS, lightDenseResidue, LIGHT_DENSE_RESIDUE, 0, 32);
        addDistillation(gasLightDenseResidue, OIL, gasLight, GAS_LIGHT, denseResidue, DENSE_RESIDUE, 1, 16);
        addDistillation(gasLightDenseResidue, OIL, gasLightDense, GAS_LIGHT_DENSE, residue, RESIDUE, 2, 12);

        addDistillation(gasLightDense, GAS_LIGHT_DENSE, gas, GAS, lightDense, LIGHT_DENSE, 0, 24);
        addDistillation(gasLightDense, GAS_LIGHT_DENSE, gasLight, GAS_LIGHT, dense, DENSE, 1, 16);

        addDistillation(gasLight, GAS_LIGHT, gas, GAS, light, LIGHT, 0, 24);

        addDistillation(lightDenseResidue, LIGHT_DENSE_RESIDUE, light, LIGHT, denseResidue, DENSE_RESIDUE, 1, 16);
        addDistillation(lightDenseResidue, LIGHT_DENSE_RESIDUE, lightDense, LIGHT_DENSE, residue, RESIDUE, 2, 12);

        addDistillation(lightDense, LIGHT_DENSE, light, LIGHT, dense, DENSE, 1, 16);

        addDistillation(denseResidue, DENSE_RESIDUE, dense, DENSE, residue, RESIDUE, 2, 12);

        for (List<BCFluid> fluid : List.of(
            gasLightDenseResidue, gasLightDense, lightDenseResidue, gasLight, lightDense, denseResidue, gas, light,
            dense, residue
        )) {
            addHeatExchange(fluid);
        }

        IRefineryRecipeManager refinery = BuildcraftRecipeRegistry.refineryRecipes;
        refinery.addHeatableRecipe(new FluidStack(Fluids.WATER, 10), null, 0, 1);
        refinery.addCoolableRecipe(new FluidStack(Fluids.LAVA, 5), null, 4, 2);
    }

    /** Splits the given fluid (at the given heat) into a gas and a liquid, in mB per operation (reduced by their common
     * factor, like in 1.12). */
    private static void addDistillation(List<BCFluid> in, int inAmount, List<BCFluid> outGas, int gasAmount,
        List<BCFluid> outLiquid, int liquidAmount, int heat, int mjCost) {
        int gcd = gcd(gcd(inAmount, gasAmount), liquidAmount);
        BuildcraftRecipeRegistry.refineryRecipes.addDistillationRecipe(
            new FluidStack(in.get(heat).get(), inAmount / gcd),
            new FluidStack(outGas.get(heat).get(), gasAmount / gcd),
            new FluidStack(outLiquid.get(heat).get(), liquidAmount / gcd),
            mjCost * MjAPI.MJ / gcd
        );
    }

    /** Each heat level of a fluid can be heated into the next one, and cooled back down. */
    private static void addHeatExchange(List<BCFluid> fluid) {
        IRefineryRecipeManager refinery = BuildcraftRecipeRegistry.refineryRecipes;
        for (int i = 0; i < fluid.size() - 1; i++) {
            BCFluid cool = fluid.get(i);
            BCFluid hot = fluid.get(i + 1);
            FluidStack coolStack = new FluidStack(cool.get(), 10);
            FluidStack hotStack = new FluidStack(hot.get(), 10);
            refinery.addHeatableRecipe(coolStack, hotStack, cool.getHeat(), hot.getHeat());
            refinery.addCoolableRecipe(hotStack, coolStack, hot.getHeat(), cool.getHeat());
        }
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** @param amountDiff How much of this fluid refining crude oil gives (the more, the less each bucket is worth).
     * @param multiplier The power given each tick, in MJ.
     * @param boostOver4 How much longer (in quarters) a bucket burns for than its share of the crude oil it came
     *            from. */
    private static void addFuel(List<BCFluid> fluid, int amountDiff, int multiplier, int boostOver4) {
        BuildcraftFuelRegistry.fuel.addFuel(
            fluid.get(0).get(), multiplier * MjAPI.MJ, burnTime(amountDiff, multiplier, boostOver4)
        );
    }

    /** A fuel that leaves residue behind as it burns. */
    private static void addDirtyFuel(List<BCFluid> fluid, int amountDiff, int multiplier, int boostOver4) {
        BuildcraftFuelRegistry.fuel.addDirtyFuel(
            fluid.get(0).get(), multiplier * MjAPI.MJ, burnTime(amountDiff, multiplier, boostOver4),
            new FluidStack(BCEnergyFluids.OIL_RESIDUE.get(0).get(), 1000 / amountDiff)
        );
    }

    private static int burnTime(int amountDiff, int multiplier, int boostOver4) {
        return TIME_BASE * boostOver4 / 4 / multiplier / amountDiff;
    }
}
