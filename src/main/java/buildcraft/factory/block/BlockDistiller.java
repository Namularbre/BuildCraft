/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileDistiller;

public class BlockDistiller extends BlockBCTile {
    /** The side of the tall input tank. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockDistiller(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST));
    }

    /** @return How far (in degrees, clockwise seen from above) the model is turned: it faces west unturned. */
    public static int modelRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 90;
            case EAST -> 180;
            case SOUTH -> 270;
            default -> 0;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileDistiller(pos, state);
    }
}
