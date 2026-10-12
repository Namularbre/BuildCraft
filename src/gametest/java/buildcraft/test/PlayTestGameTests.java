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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.fluid.BCFluid;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileEngineIron;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TileTank;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.tile.TilePipeHolder;

/** Bugs found while playing. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class PlayTestGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final String BIG = "gametest/empty5x5x5";

    /** Buckets of BuildCraft fluids (oil, fuel...) empty into tanks. */
    @GameTest(template = EMPTY)
    public static void modBucketsEmptyIntoTanks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCFactoryBlocks.TANK.get());
        TileTank tank = (TileTank) helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer();
        BlockPos absolute = helper.absolutePos(pos);
        for (BCFluid fluid : new BCFluid[] { BCEnergyFluids.CRUDE_OIL.get(0), BCEnergyFluids.FUEL_LIGHT.get(0) }) {
            tank.tank.setFluid(FluidStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fluid.bucket.get()));
            tank.onActivated(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
            boolean filled = tank.tank.getFluidAmount() == 1_000 && tank.tank.getFluid().getFluid().isSame(fluid.get());
            helper.assertTrue(filled, "The bucket of " + fluid.name + " should empty into the tank");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "The player should get the bucket back");
        }
        helper.succeed();
    }

    /** Buckets of fuel fill the combustion engine (instead of opening its GUI). */
    @GameTest(template = EMPTY)
    public static void fuelBucketsFillCombustionEngines(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCEnergyBlocks.ENGINE_IRON.get());
        TileEngineIron engine = (TileEngineIron) helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer();
        BCFluid fuel = BCEnergyFluids.FUEL_LIGHT.get(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fuel.bucket.get()));
        BlockPos absolute = helper.absolutePos(pos);
        engine.onActivated(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        helper.assertTrue(engine.tankFuel.getFluidAmount() == 1_000, "The fuel should go in the engine");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "The player should get the bucket back");
        helper.succeed();
    }

    /** Oil flowing onto pipes (and other BuildCraft blocks) doesn't destroy them. */
    @GameTest(template = BIG, timeoutTicks = 100)
    public static void fluidsDontDestroyPipes(GameTestHelper helper) {
        BlockPos pipe = new BlockPos(2, 1, 2);
        helper.setBlock(pipe, BCTransportBlocks.PIPE_HOLDER.get());
        ((TilePipeHolder) helper.getBlockEntity(pipe)).onPlacedBy(
            helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get())
        );
        helper.setBlock(new BlockPos(2, 1, 3), BCFactoryBlocks.TANK.get());
        helper.setBlock(pipe.above(), BCEnergyFluids.CRUDE_OIL.get(0).block.get());
        helper.runAfterDelay(80, () -> {
            helper.assertBlockPresent(BCTransportBlocks.PIPE_HOLDER.get(), pipe);
            helper.assertBlockPresent(BCFactoryBlocks.TANK.get(), new BlockPos(2, 1, 3));
            helper.succeed();
        });
    }

    /** Item pipes next to a mining well connect to it, and get what it mines. */
    @GameTest(template = BIG)
    public static void miningWellsFeedPipes(GameTestHelper helper) {
        BlockPos well = new BlockPos(2, 2, 2);
        BlockPos pipe = well.east();
        helper.setBlock(well.below(), Blocks.DIRT);
        helper.setBlock(well, BCFactoryBlocks.MINING_WELL.get());
        helper.setBlock(pipe, BCTransportBlocks.PIPE_HOLDER.get());
        ((TilePipeHolder) helper.getBlockEntity(pipe)).onPlacedBy(
            helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get())
        );
        TileMiningWell miningWell = (TileMiningWell) helper.getBlockEntity(well);
        IMjReceiver receiver = miningWell.getCapability(MjAPI.CAP_RECEIVER, Direction.UP)
            .orElseThrow(IllegalStateException::new);
        helper.onEachTick(() -> receiver.receivePower(50 * MjAPI.MJ, false));
        helper.succeedWhen(() -> {
            TilePipeHolder holder = (TilePipeHolder) helper.getBlockEntity(pipe);
            helper.assertTrue(holder.getPipe().isConnected(Direction.WEST), "The pipe should connect to the well");
            helper.assertBlockPresent(Blocks.AIR, well.below());
            helper.assertTrue(holder.getPipe().getFlow() instanceof PipeFlowItems flow && flow.doesContainItems(),
                "The dirt should be in the pipe");
        });
    }
}
