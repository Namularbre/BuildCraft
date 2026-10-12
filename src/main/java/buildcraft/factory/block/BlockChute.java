/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileChute;

public class BlockChute extends BlockBCTile {
    /** The side of the opening, where items are picked up. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** For each side: true if items are sent to an inventory there (drawn as a small spout). */
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;

    public BlockChute(Properties properties) {
        super(properties.noOcclusion());
        BlockState state = stateDefinition.any().setValue(FACING, Direction.UP);
        for (BooleanProperty connected : CONNECTED.values()) {
            state = state.setValue(connected, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
        BlockPos pos, BlockPos neighbourPos) {
        return updateConnections(state, level, pos);
    }

    /** Turning the chute (with a wrench) changes which sides it sends items to. */
    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        BlockState updated = updateConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (Direction side : Direction.values()) {
            boolean connected = side != facing && TileChute.hasInventoryAtPosition(level, pos.relative(side), side);
            state = state.setValue(CONNECTED.get(side), connected);
        }
        return state;
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
        return new TileChute(pos, state);
    }
}
