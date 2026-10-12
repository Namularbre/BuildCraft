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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.MjAPI;

import buildcraft.lib.engine.BlockEngineBase;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.core.tile.TilePowerConsumerTester;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileEngineIron;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class CombustionEngineGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    /** A combustion engine burning light fuel, cooled by water, powers kinesis pipes up to a machine. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void combustionEnginePowersKinesisPipes(GameTestHelper helper) {
        BlockPos wood = new BlockPos(1, 1, 1);
        BlockPos stone = new BlockPos(2, 1, 1);
        BlockPos tester = new BlockPos(2, 1, 2);
        BlockPos engine = wood.above();
        placePipe(helper, wood, BCTransportItems.PIPE_WOOD_POWER.get());
        placePipe(helper, stone, BCTransportItems.PIPE_STONE_POWER.get());
        helper.setBlock(tester, BCCoreBlocks.POWER_TESTER.get());

        helper.runAfterDelay(2, () -> {
            helper.setBlock(engine, BCEnergyBlocks.ENGINE_IRON.get());
            TileEngineIron tile = (TileEngineIron) helper.getBlockEntity(engine);
            tile.getFluidHandler().fill(fuel(1000), FluidAction.EXECUTE);
            tile.getFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
            helper.setBlock(engine.north(), Blocks.REDSTONE_BLOCK);
            helper.assertBlockProperty(engine, BlockEngineBase.FACING, Direction.DOWN);

            helper.succeedWhen(() -> {
                long received = ((TilePowerConsumerTester) helper.getBlockEntity(tester)).getTotalReceived();
                helper.assertTrue(received >= 10 * MjAPI.MJ, "The tester should have received power, got "
                    + MjAPI.formatMj(received) + " MJ");
                helper.assertTrue(tile.tankFuel.getFluidAmount() < 1000, "The engine should have burnt fuel");
            });
        });
    }

    /** Burning crude oil leaves residue, which can be taken out of the engine. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void crudeOilLeavesResidue(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_IRON.get());
        TileEngineIron tile = (TileEngineIron) helper.getBlockEntity(engine);
        tile.getFluidHandler().fill(
            new FluidStack(BCEnergyFluids.CRUDE_OIL.get(0).get(), 1000), FluidAction.EXECUTE
        );
        tile.getFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        helper.setBlock(engine.west(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            FluidStack drained = tile.getFluidHandler().drain(1000, FluidAction.SIMULATE);
            helper.assertTrue(
                !drained.isEmpty() && drained.getFluid() == BCEnergyFluids.OIL_RESIDUE.get(0).get(),
                "Residue should be drainable from the engine"
            );
        });
    }

    /** Fuel and coolant go in their own tanks, and anything else is refused. */
    @GameTest(template = EMPTY)
    public static void fluidsGoInTheRightTank(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_IRON.get());
        TileEngineIron tile = (TileEngineIron) helper.getBlockEntity(engine);

        helper.assertTrue(tile.getFluidHandler().fill(fuel(500), FluidAction.EXECUTE) == 500, "Fuel should fit");
        helper.assertTrue(tile.tankFuel.getFluidAmount() == 500, "Fuel should be in the fuel tank");
        tile.getFluidHandler().fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        helper.assertTrue(tile.tankCoolant.getFluidAmount() == 500, "Water should be in the coolant tank");
        int lava = tile.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
        helper.assertTrue(lava == 0, "Lava is neither a fuel nor a coolant");
        helper.assertTrue(
            tile.getFluidHandler().drain(100, FluidAction.SIMULATE).isEmpty(), "Only residue can be drained"
        );
        helper.succeed();
    }

    /** Ice melts straight into the coolant tank. */
    @GameTest(template = EMPTY)
    public static void iceCoolsTheEngine(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_IRON.get());
        TileEngineIron tile = (TileEngineIron) helper.getBlockEntity(engine);

        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ICE, 2));
        tile.onActivated(player, InteractionHand.MAIN_HAND, hit(helper, engine));
        helper.assertTrue(tile.tankCoolant.getFluidAmount() == 1500, "1 ice should melt into 1500 mB of water");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "1 ice should have been used");
        helper.succeed();
    }

    /** Holding a wrench lets it rotate the engine rather than open the GUI. */
    @GameTest(template = EMPTY)
    public static void wrenchPassesThrough(GameTestHelper helper) {
        BlockPos engine = new BlockPos(1, 1, 1);
        helper.setBlock(engine, BCEnergyBlocks.ENGINE_IRON.get());
        TileEngineIron tile = (TileEngineIron) helper.getBlockEntity(engine);

        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BCCoreItems.WRENCH.get()));
        InteractionResult result = tile.onActivated(player, InteractionHand.MAIN_HAND, hit(helper, engine));
        helper.assertTrue(result == InteractionResult.PASS, "The wrench should be used instead, got " + result);
        helper.succeed();
    }

    private static FluidStack fuel(int amount) {
        return new FluidStack(BCEnergyFluids.FUEL_LIGHT.get(0).get(), amount);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
    }

    private static void placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
    }
}
