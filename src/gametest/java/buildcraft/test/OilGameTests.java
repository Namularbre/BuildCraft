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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.lib.fluid.BCFluid;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class OilGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** Every oil product exists in 3 heat levels, and the ones hotter than their boiling point are gases. */
    @GameTest(template = EMPTY)
    public static void oilFluidsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(BCEnergyFluids.ALL.size() == 30, "There should be 30 oil fluids, found "
            + BCEnergyFluids.ALL.size());
        for (int heat = 0; heat < 3; heat++) {
            BCFluid oil = BCEnergyFluids.CRUDE_OIL.get(heat);
            helper.assertTrue(oil.getHeat() == heat, oil.name + " should have heat " + heat);
            helper.assertFalse(oil.type.get().isLighterThanAir(), "Crude oil never boils");
            helper.assertTrue(
                BCEnergyFluids.FUEL_GASEOUS.get(heat).type.get().isLighterThanAir(), "Gaseous fuel is always a gas"
            );
        }
        // Light fuel boils at heat 1
        helper.assertFalse(BCEnergyFluids.FUEL_LIGHT.get(0).type.get().isLighterThanAir(), "Cool light fuel");
        helper.assertTrue(BCEnergyFluids.FUEL_LIGHT.get(1).type.get().isLighterThanAir(), "Hot light fuel");
        helper.succeed();
    }

    /** Names show the heat level around the fluid's name. */
    @GameTest(template = EMPTY)
    public static void oilNamesShowTheirHeat(GameTestHelper helper) {
        BCFluid hotOil = BCEnergyFluids.CRUDE_OIL.get(1);
        TranslatableContents name = (TranslatableContents) hotOil.type.get().getDescription().getContents();
        helper.assertTrue(name.getKey().equals("buildcraft.fluid.heat_1"), "Unexpected name key " + name.getKey());
        TranslatableContents inner = (TranslatableContents) ((Component) name.getArgs()[0]).getContents();
        helper.assertTrue(inner.getKey().equals("fluid.buildcraft.oil"), "Unexpected fluid key " + inner.getKey());
        TranslatableContents bucket = (TranslatableContents) hotOil.bucket.get().getName(
            new ItemStack(hotOil.bucket.get())
        ).getContents();
        helper.assertTrue(bucket.getKey().equals("item.buildcraft.fluid_bucket"), "Unexpected bucket key");
        helper.succeed();
    }

    /** Oil placed in the world flows like a liquid. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void oilFlowsInTheWorld(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BCEnergyFluids.CRUDE_OIL.get(0).block.get());
        helper.succeedWhen(() -> {
            FluidState below = helper.getLevel().getFluidState(helper.absolutePos(pos.below()));
            helper.assertTrue(below.getType().isSame(BCEnergyFluids.CRUDE_OIL.get(0).get()), "Oil should flow down");
        });
    }

    /** Oil burns (by default), but residue doesn't. */
    @GameTest(template = EMPTY)
    public static void oilIsFlammable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCEnergyFluids.CRUDE_OIL.get(0).block.get());
        helper.assertTrue(helper.getBlockState(pos).isFlammable(
            helper.getLevel(), helper.absolutePos(pos), Direction.UP
        ), "Oil should be flammable");
        helper.setBlock(pos, BCEnergyFluids.OIL_RESIDUE.get(0).block.get());
        helper.assertFalse(helper.getBlockState(pos).isFlammable(
            helper.getLevel(), helper.absolutePos(pos), Direction.UP
        ), "Residue shouldn't be flammable");
        helper.succeed();
    }

    /** Oil goes through fluid pipes like any other fluid. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void oilGoesThroughPipes(GameTestHelper helper) {
        BlockPos first = new BlockPos(0, 1, 1);
        BlockPos second = new BlockPos(1, 1, 1);
        placePipe(helper, first);
        placePipe(helper, second);
        helper.runAfterDelay(2, () -> {
            PipeFlowFluids flow = fluids(helper, first);
            int inserted = flow.insertFluidsForce(new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 20), null,
                false);
            helper.assertTrue(inserted > 0, "The pipe should accept oil");
            helper.succeedWhen(() -> helper.assertTrue(fluids(helper, second).getTotalAmount() > 0,
                "Oil should reach the second pipe"));
        });
    }

    private static PipeFlowFluids fluids(GameTestHelper helper, BlockPos pos) {
        return (PipeFlowFluids) ((TilePipeHolder) helper.getBlockEntity(pos)).getPipe().getFlow();
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_FLUID.get()));
    }
}
