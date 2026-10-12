/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FluidPipeGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** A wooden fluid pipe powered by a redstone engine pumps water out of a tank, and pipes carry it to another. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void woodPipeMovesFluidBetweenTanks(GameTestHelper helper) {
        BlockPos source = new BlockPos(0, 1, 1);
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        BlockPos target = new BlockPos(2, 1, 2);

        helper.setBlock(source, TestTank.block);
        helper.setBlock(target, TestTank.block);
        tank(helper, source).tank.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.EXECUTE);
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_FLUID.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_FLUID.get());
        helper.setBlock(wood.above(), BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(wood.above().north(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            int arrived = tank(helper, target).tank.getFluidAmount();
            helper.assertTrue(arrived >= 1000, "At least a bucket should have arrived, found " + arrived + " mB");
            int left = tank(helper, source).tank.getFluidAmount();
            int inPipes = fluids(helper, wood).getTotalAmount() + fluids(helper, stone).getTotalAmount();
            helper.assertTrue(
                left + inPipes + arrived == 2000, "No water should be created or lost: " + left + " + " + inPipes
                    + " + " + arrived + " mB"
            );
        });
    }

    /** Fluid put straight into a pipe spreads into the pipes connected to it. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void fluidSpreadsBetweenPipes(GameTestHelper helper) {
        BlockPos first = new BlockPos(0, 1, 1);
        BlockPos second = new BlockPos(1, 1, 1);
        placePipe(helper, first, BCTransportItems.PIPE_STONE_FLUID.get());
        placePipe(helper, second, BCTransportItems.PIPE_STONE_FLUID.get());

        helper.runAfterDelay(2, () -> {
            int inserted = fluids(helper, first).insertFluidsForce(new FluidStack(Fluids.WATER, 100), null, false);
            helper.assertTrue(inserted > 0, "The pipe should accept water");
            helper.succeedWhen(() -> {
                helper.assertTrue(fluids(helper, second).getTotalAmount() > 0, "Water should reach the second pipe");
            });
        });
    }

    /** Fluid that reaches the center of a void pipe disappears. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void voidPipeDestroysFluid(GameTestHelper helper) {
        BlockPos stone = new BlockPos(0, 1, 1);
        BlockPos voidPipe = new BlockPos(1, 1, 1);
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_FLUID.get());
        placePipe(helper, voidPipe, BCTransportItems.PIPE_VOID_FLUID.get());

        helper.runAfterDelay(2, () -> {
            fluids(helper, stone).insertFluidsForce(new FluidStack(Fluids.WATER, 100), null, false);
            helper.succeedWhen(() -> {
                int total = fluids(helper, stone).getTotalAmount() + fluids(helper, voidPipe).getTotalAmount();
                helper.assertTrue(total == 0, "All the water should have been destroyed, " + total + " mB left");
            });
        });
    }

    /** The fluid inside a pipe survives being saved and loaded. */
    @GameTest(template = EMPTY)
    public static void fluidPipeSavesItsContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        placePipe(helper, pos, BCTransportItems.PIPE_GOLD_FLUID.get());
        helper.runAfterDelay(2, () -> {
            // A pipe only accepts its flow rate in a single tick (80 mB for gold)
            int inserted = fluids(helper, pos).insertFluidsForce(new FluidStack(Fluids.LAVA, 250), null, false);
            helper.assertTrue(inserted > 0, "The pipe should accept lava");
            TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
            CompoundTag saved = tile.saveWithoutMetadata();

            TilePipeHolder copy = new TilePipeHolder(helper.absolutePos(pos), tile.getBlockState());
            copy.load(saved);
            int amount = ((PipeFlowFluids) copy.getPipe().getFlow()).getTotalAmount();
            helper.assertTrue(amount == inserted, "The pipe should still contain " + inserted + " mB, found " + amount);
            helper.succeed();
        });
    }

    private static TestTank.Tile tank(GameTestHelper helper, BlockPos pos) {
        return (TestTank.Tile) helper.getBlockEntity(pos);
    }

    private static PipeFlowFluids fluids(GameTestHelper helper, BlockPos pos) {
        return (PipeFlowFluids) ((TilePipeHolder) helper.getBlockEntity(pos)).getPipe().getFlow();
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
    }
}
