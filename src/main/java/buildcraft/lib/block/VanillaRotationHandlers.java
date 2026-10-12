/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** Rotation of blocks that don't have a {@link buildcraft.api.blocks.ICustomRotationHandler}, based on their standard
 * facing or axis properties. */
public final class VanillaRotationHandlers {
    /** The order in which the wrench cycles through every direction. */
    public static final List<Direction> ROTATE_FACING = List.of(
        Direction.EAST, Direction.SOUTH, Direction.DOWN, Direction.WEST, Direction.NORTH, Direction.UP
    );
    /** The order in which the wrench cycles through the horizontal directions. */
    public static final List<Direction> ROTATE_HORIZONTAL = List.of(
        Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH
    );

    private VanillaRotationHandlers() {}

    public static Direction next(List<Direction> order, Direction current) {
        int index = order.indexOf(current);
        return order.get((index + 1) % order.size());
    }

    /** Rotates a block with a standard facing or axis property to the next valid orientation.
     *
     * @return {@link InteractionResult#PASS} if the block doesn't have a property we know how to rotate safely. */
    public static InteractionResult rotateGeneric(Level level, BlockPos pos, BlockState state) {
        if (isMultiBlock(state)) {
            return InteractionResult.PASS;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return rotateFacing(level, pos, state, BlockStateProperties.FACING, ROTATE_FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return rotateFacing(level, pos, state, BlockStateProperties.HORIZONTAL_FACING, ROTATE_HORIZONTAL);
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            BlockState rotated = state.cycle(BlockStateProperties.AXIS);
            return setIfValid(level, pos, rotated);
        }
        return InteractionResult.PASS;
    }

    /** Blocks made of several parts (beds, doors, double chests, extended pistons) would break if only one part was
     * rotated. */
    private static boolean isMultiBlock(BlockState state) {
        if (state.hasProperty(BlockStateProperties.BED_PART)) return true;
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return true;
        if (state.hasProperty(BlockStateProperties.CHEST_TYPE)
            && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) return true;
        return state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED);
    }

    private static InteractionResult rotateFacing(Level level, BlockPos pos, BlockState state,
        DirectionProperty property, List<Direction> order) {
        Direction current = state.getValue(property);
        for (int i = 0; i < order.size() - 1; i++) {
            current = next(order, current);
            if (!property.getPossibleValues().contains(current)) {
                continue;
            }
            BlockState rotated = state.setValue(property, current);
            if (rotated.canSurvive(level, pos)) {
                return setIfValid(level, pos, rotated);
            }
        }
        return InteractionResult.FAIL;
    }

    private static InteractionResult setIfValid(Level level, BlockPos pos, BlockState rotated) {
        if (!rotated.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, rotated, Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }
}
