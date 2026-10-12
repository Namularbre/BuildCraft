/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TilePump;
import buildcraft.factory.tile.TileTank;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class PumpGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";
    private static final BlockPos PUMP = new BlockPos(2, 4, 2);
    private static final BlockPos TANK = PUMP.east();
    /** The pool is 3 by 3, one block deep, at this height. */
    private static final int POOL_Y = 1;

    /** A powered pump drains a pool of oil into the tank next to it, then pulls its tube back up. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pumpDrainsAPool(GameTestHelper helper) {
        FlowingFluid oil = BCEnergyFluids.CRUDE_OIL.get(0).get();
        buildPool(helper, oil);
        TilePump pump = placePumpAndTank(helper);
        IMjReceiver receiver = receiver(helper);
        helper.onEachTick(() -> receiver.receivePower(5 * MjAPI.MJ, false));

        helper.succeedWhen(() -> {
            TileTank tank = (TileTank) helper.getBlockEntity(TANK);
            int amount = tank.tank.getFluidAmount();
            helper.assertTrue(amount == 9_000, "The tank should hold the 9 buckets of the pool, has " + amount);
            helper.assertTrue(tank.tank.getFluid().getFluid().isSame(oil), "The tank should hold oil");
            forEachPoolBlock(pos -> helper.assertTrue(
                helper.getLevel().getFluidState(helper.absolutePos(pos)).isEmpty(), "The pool should be empty at " + pos
            ));
            helper.assertTrue(pump.isComplete(), "The pump should have nothing left to pump");
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), PUMP.below());
        });
    }

    /** The pump goes down through air to reach the fluid, placing tube blocks, which are removed with the pump. */
    @GameTest(template = EMPTY)
    public static void tubeIsRemovedWithThePump(GameTestHelper helper) {
        buildPool(helper, Fluids.LAVA);
        placePumpAndTank(helper);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCFactoryBlocks.TUBE.get(), PUMP.below());
            helper.assertBlockPresent(BCFactoryBlocks.TUBE.get(), PUMP.below(2));
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), PUMP.below(3));
            helper.setBlock(PUMP, Blocks.AIR);
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), PUMP.below());
            helper.assertBlockNotPresent(BCFactoryBlocks.TUBE.get(), PUMP.below(2));
        });
    }

    /** A pool of water that refills itself is pumped without removing any water (with the default config). */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void infiniteWaterIsNotConsumed(GameTestHelper helper) {
        buildPool(helper, Fluids.WATER);
        TilePump pump = placePumpAndTank(helper);
        IMjReceiver receiver = receiver(helper);
        helper.onEachTick(() -> receiver.receivePower(5 * MjAPI.MJ, false));

        helper.succeedWhen(() -> {
            TileTank tank = (TileTank) helper.getBlockEntity(TANK);
            int amount = tank.tank.getFluidAmount();
            helper.assertTrue(amount >= 12_000, "The pump should keep pumping water, pumped " + amount);
            helper.assertTrue(pump.isInfiniteWaterSource(), "The pool should be seen as an infinite source");
            forEachPoolBlock(pos -> helper.assertTrue(
                helper.getLevel().getFluidState(helper.absolutePos(pos)).isSource(), "The pool should be full at " + pos
            ));
        });
    }

    /** Pumps accept power from redstone engines, like other machines that only need a little power. */
    @GameTest(template = EMPTY)
    public static void pumpAcceptsRedstonePower(GameTestHelper helper) {
        helper.setBlock(PUMP, BCFactoryBlocks.PUMP.get());
        boolean present = helper.getBlockEntity(PUMP)
            .getCapability(MjAPI.CAP_REDSTONE_RECEIVER, Direction.UP).isPresent();
        helper.assertTrue(present, "The pump should accept redstone engine power");
        helper.succeed();
    }

    /** A stone basin with a 3 by 3 pool of source blocks inside, and air above it. */
    private static void buildPool(GameTestHelper helper, Fluid fluid) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, POOL_Y - 1, z), Blocks.STONE);
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                helper.setBlock(new BlockPos(x, POOL_Y, z), edge ? Blocks.STONE.defaultBlockState()
                    : fluid.defaultFluidState().createLegacyBlock());
            }
        }
    }

    private static void forEachPoolBlock(Consumer<BlockPos> action) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                action.accept(new BlockPos(x, POOL_Y, z));
            }
        }
    }

    private static TilePump placePumpAndTank(GameTestHelper helper) {
        helper.setBlock(PUMP, BCFactoryBlocks.PUMP.get());
        helper.setBlock(TANK, BCFactoryBlocks.TANK.get());
        return (TilePump) helper.getBlockEntity(PUMP);
    }

    private static IMjReceiver receiver(GameTestHelper helper) {
        return helper.getBlockEntity(PUMP).getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(IllegalStateException::new);
    }
}
