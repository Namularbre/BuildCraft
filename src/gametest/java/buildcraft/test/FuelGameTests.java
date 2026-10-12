/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager.IDirtyFuel;
import buildcraft.api.fuels.ISolidCoolant;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.fluid.BCFluid;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FuelGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** The fuels have the same values as in 1.12. */
    @GameTest(template = EMPTY)
    public static void fuelValues(GameTestHelper helper) {
        assertFuel(helper, BCEnergyFluids.CRUDE_OIL.get(0), 3, 10_000);
        assertFuel(helper, BCEnergyFluids.FUEL_LIGHT.get(0), 6, 15_000);
        assertFuel(helper, BCEnergyFluids.FUEL_GASEOUS.get(0), 8, 1_875);
        assertFuel(helper, BCEnergyFluids.FUEL_DENSE.get(0), 4, 90_000);
        helper.succeed();
    }

    /** Crude oil leaves residue behind as it burns: 125 mB per bucket. */
    @GameTest(template = EMPTY)
    public static void oilLeavesResidue(GameTestHelper helper) {
        IFuel oil = BuildcraftFuelRegistry.fuel.getFuel(new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1));
        helper.assertTrue(oil instanceof IDirtyFuel, "Crude oil should be a dirty fuel");
        FluidStack residue = ((IDirtyFuel) oil).getResidue();
        helper.assertTrue(
            residue.getFluid() == BCEnergyFluids.OIL_RESIDUE.get(0).get() && residue.getAmount() == 125,
            "Crude oil should leave 125 mB of residue per bucket, found " + residue.getAmount()
        );
        helper.succeed();
    }

    /** Only the cool variant of each fluid burns in engines. */
    @GameTest(template = EMPTY)
    public static void hotFluidsAreNotFuels(GameTestHelper helper) {
        for (int heat = 1; heat < 3; heat++) {
            BCFluid hot = BCEnergyFluids.FUEL_LIGHT.get(heat);
            helper.assertTrue(
                BuildcraftFuelRegistry.fuel.getFuel(new FluidStack(hot.get(), 1)) == null, hot.name + " isn't a fuel"
            );
        }
        helper.succeed();
    }

    /** Water cools engines, and ice melts into more water than it takes up. */
    @GameTest(template = EMPTY)
    public static void coolants(GameTestHelper helper) {
        float waterCooling = BuildcraftFuelRegistry.coolant.getDegreesPerMb(new FluidStack(Fluids.WATER, 1), 0);
        helper.assertTrue(waterCooling == 0.0023f, "Water should cool 0.0023 degrees per mB, found " + waterCooling);

        ItemStack ice = new ItemStack(Items.ICE);
        ISolidCoolant solid = BuildcraftFuelRegistry.coolant.getSolidCoolant(ice);
        helper.assertTrue(solid != null, "Ice should be a solid coolant");
        FluidStack melted = solid.getFluidFromSolidCoolant(ice);
        helper.assertTrue(melted.getAmount() == 1500, "Ice should melt into 1500 mB, found " + melted.getAmount());

        ItemStack packedIce = new ItemStack(Items.PACKED_ICE);
        FluidStack packedMelted = BuildcraftFuelRegistry.coolant.getSolidCoolant(packedIce)
            .getFluidFromSolidCoolant(packedIce);
        helper.assertTrue(packedMelted.getAmount() == 2000, "Packed ice should melt into 2000 mB");
        helper.succeed();
    }

    private static void assertFuel(GameTestHelper helper, BCFluid fluid, int mjPerTick, int ticksPerBucket) {
        IFuel fuel = BuildcraftFuelRegistry.fuel.getFuel(new FluidStack(fluid.get(), 1));
        helper.assertTrue(fuel != null, fluid.name + " should be a fuel");
        helper.assertTrue(fuel.getPowerPerCycle() == mjPerTick * MjAPI.MJ, fluid.name + " should give " + mjPerTick
            + " MJ/t, gives " + MjAPI.formatMj(fuel.getPowerPerCycle()));
        helper.assertTrue(fuel.getTotalBurningTime() == ticksPerBucket, fluid.name + " should burn for "
            + ticksPerBucket + " ticks, burns for " + fuel.getTotalBurningTime());
    }
}
