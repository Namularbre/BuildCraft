/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The frame around a quarry: a thin bar joined to the frame and quarry next to it. */
public class BlockFrame extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
        Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12),
        Direction.UP, Block.box(4, 12, 4, 12, 16, 12),
        Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4),
        Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
        Direction.WEST, Block.box(0, 4, 4, 4, 12, 12),
        Direction.EAST, Block.box(12, 4, 4, 16, 12, 12)
    );

    public BlockFrame(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty connected : CONNECTED.values()) {
            state = state.setValue(connected, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
        BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(CONNECTED.get(direction), connects(neighbour));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
        boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        BlockState updated = updateConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTED.get(side), connects(level.getBlockState(pos.relative(side))));
        }
        return state;
    }

    private static boolean connects(BlockState neighbour) {
        return neighbour.getBlock() instanceof BlockFrame || neighbour.getBlock() instanceof BlockQuarry;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        for (Direction side : Direction.values()) {
            if (state.getValue(CONNECTED.get(side))) {
                shape = Shapes.or(shape, ARMS.get(side));
            }
        }
        return shape;
    }
}
