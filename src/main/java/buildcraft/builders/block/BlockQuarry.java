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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import net.minecraftforge.common.capabilities.ForgeCapabilities;

import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.builders.tile.TileQuarry;

public class BlockQuarry extends BlockBCTile {
    /** The front of the quarry: its area is behind it. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** For each side of the model (north being the front): true if an inventory or pipe is there (drawn as an item
     * hatch). */
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;

    public BlockQuarry(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
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

    /** @return How far (in degrees, clockwise seen from above) the model is turned: it faces north unturned. */
    public static int modelRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return updateConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
        BlockPos pos, BlockPos neighbourPos) {
        return updateConnections(state, level, pos);
    }

    private static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Rotation rotation = switch (state.getValue(FACING)) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        for (Direction local : Direction.values()) {
            Direction world = rotation.rotate(local);
            BlockEntity tile = level.getBlockEntity(pos.relative(world));
            boolean connected = tile != null && !(tile instanceof TileQuarry)
                && (tile.getCapability(ForgeCapabilities.ITEM_HANDLER, world.getOpposite()).isPresent()
                    || tile.getCapability(PipeApi.CAP_INJECTABLE, world.getOpposite()).isPresent());
            state = state.setValue(CONNECTED.get(local), connected);
        }
        return state;
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileQuarry(pos, state);
    }
}
