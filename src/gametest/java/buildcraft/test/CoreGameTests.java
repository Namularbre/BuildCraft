/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.lib.engine.BlockEngineBase;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.core.tile.TilePowerConsumerTester;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class CoreGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    private static final BlockPos ENGINE = new BlockPos(1, 1, 1);

    /** A redstone engine with a redstone signal pushes power into the receiver it faces. */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void redstoneEnginePowersReceiver(GameTestHelper helper) {
        helper.setBlock(ENGINE.above(), BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(ENGINE, BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(ENGINE.west(), Blocks.REDSTONE_BLOCK);

        helper.succeedWhen(() -> {
            TilePowerConsumerTester tester = (TilePowerConsumerTester) helper.getBlockEntity(ENGINE.above());
            helper.assertTrue(tester.getTotalReceived() > 0, "The tester should have received power");
        });
    }

    /** Without a redstone signal, the engine doesn't produce anything. */
    @GameTest(template = EMPTY)
    public static void redstoneEngineNeedsSignal(GameTestHelper helper) {
        helper.setBlock(ENGINE.above(), BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(ENGINE, BCCoreBlocks.ENGINE_REDSTONE.get());

        helper.runAfterDelay(80, () -> {
            TilePowerConsumerTester tester = (TilePowerConsumerTester) helper.getBlockEntity(ENGINE.above());
            helper.assertTrue(tester.getTotalReceived() == 0, "The tester shouldn't have received power");
            helper.succeed();
        });
    }

    /** An engine turns to face the only receiver next to it. */
    @GameTest(template = EMPTY)
    public static void engineTurnsToReceiver(GameTestHelper helper) {
        helper.setBlock(ENGINE, BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(ENGINE.west(), BCCoreBlocks.POWER_TESTER.get());

        helper.assertBlockProperty(ENGINE, BlockEngineBase.FACING, Direction.WEST);
        helper.succeed();
    }

    /** The wrench rotates vanilla blocks with a horizontal facing. */
    @GameTest(template = EMPTY)
    public static void wrenchRotatesFurnace(GameTestHelper helper) {
        helper.setBlock(ENGINE, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        useWrench(helper, ENGINE, Direction.UP);
        helper.assertBlockProperty(ENGINE, FurnaceBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    /** The wrench cycles an engine between the receivers around it. */
    @GameTest(template = EMPTY)
    public static void wrenchRotatesEngine(GameTestHelper helper) {
        helper.setBlock(ENGINE.above(), BCCoreBlocks.POWER_TESTER.get());
        helper.setBlock(ENGINE, BCCoreBlocks.ENGINE_REDSTONE.get());
        helper.setBlock(ENGINE.east(), BCCoreBlocks.POWER_TESTER.get());
        // Still facing the first receiver
        helper.assertBlockProperty(ENGINE, BlockEngineBase.FACING, Direction.UP);

        useWrench(helper, ENGINE, Direction.NORTH);
        helper.assertBlockProperty(ENGINE, BlockEngineBase.FACING, Direction.EAST);
        useWrench(helper, ENGINE, Direction.NORTH);
        helper.assertBlockProperty(ENGINE, BlockEngineBase.FACING, Direction.UP);
        helper.succeed();
    }

    /** The engine renderer relies on {@link Direction#getRotation()} turning "up" into the engine's facing, like the
     * block state rotations do. */
    @GameTest(template = EMPTY)
    public static void directionRotationTurnsUpToFacing(GameTestHelper helper) {
        for (Direction dir : Direction.values()) {
            Vector3f up = dir.getRotation().transform(new Vector3f(0, 1, 0));
            Vector3f expected = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
            helper.assertTrue(up.distance(expected) < 1e-5, dir + " rotated up to " + up);
        }
        helper.succeed();
    }

    private static void useWrench(GameTestHelper helper, BlockPos relative, Direction side) {
        Player player = helper.makeMockPlayer();
        ItemStack wrench = new ItemStack(BCCoreItems.WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false);
        wrench.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, wrench, hit));
    }
}
