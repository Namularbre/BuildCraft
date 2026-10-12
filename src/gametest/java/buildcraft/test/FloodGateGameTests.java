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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreItems;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.tile.TileFloodGate;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FloodGateGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";
    /** Just above the middle of a 3 by 3 stone basin. */
    private static final BlockPos GATE = new BlockPos(2, 2, 2);

    /** A flood gate open only at the bottom fills the basin below it with lava source blocks, and nothing else. */
    @GameTest(template = EMPTY, timeoutTicks = 600)
    public static void floodGateFillsABasin(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                helper.setBlock(new BlockPos(x, 1, z), edge ? Blocks.STONE : Blocks.AIR);
            }
        }
        BlockState state = BCFactoryBlocks.FLOOD_GATE.get().defaultBlockState();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            state = state.setValue(BlockFloodGate.OPEN.get(side), false);
        }
        helper.setBlock(GATE, state);
        TileFloodGate gate = (TileFloodGate) helper.getBlockEntity(GATE);
        helper.onEachTick(() -> gate.tank.fill(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE));

        helper.succeedWhen(() -> {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    BlockPos pos = new BlockPos(x, 1, z);
                    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).isSource(),
                        "There should be a lava source at " + pos);
                }
            }
            for (Direction side : Direction.Plane.HORIZONTAL) {
                helper.assertBlockPresent(Blocks.AIR, GATE.relative(side));
            }
        });
    }

    /** A wrench opens and closes the side it is used on, but never the top. */
    @GameTest(template = EMPTY)
    public static void wrenchTogglesSides(GameTestHelper helper) {
        helper.setBlock(GATE, BCFactoryBlocks.FLOOD_GATE.get());
        TileFloodGate gate = (TileFloodGate) helper.getBlockEntity(GATE);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BCCoreItems.WRENCH.get()));
        BlockPos absolute = helper.absolutePos(GATE);

        gate.onActivated(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        helper.assertBlockProperty(GATE, BlockFloodGate.OPEN.get(Direction.NORTH), false);
        helper.assertBlockProperty(GATE, BlockFloodGate.OPEN.get(Direction.SOUTH), true);

        gate.onActivated(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
        helper.assertBlockProperty(GATE, BlockFloodGate.OPEN.get(Direction.NORTH), true);
        helper.assertTrue(!gate.isOpen(Direction.UP), "The top is always closed");
        helper.succeed();
    }
}
