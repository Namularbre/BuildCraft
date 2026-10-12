/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.transport.pipe.ICustomPipeConnection;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileHeatExchange;

/** Heat exchangers are built from 3 to 5 blocks in a row facing the same way: the first block (the "start") takes in
 * the fluid to heat from below, and the last block (the "end") takes in the fluid to cool from its side. */
public class BlockHeatExchange extends BlockBCTile implements ICustomPipeConnection, ICustomRotationHandler {
    public enum EnumExchangePart implements StringRepresentable {
        START,
        MIDDLE,
        END;

        private final String name = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Set by the block entity, once it knows which part of a heat exchanger it is. */
    public static final EnumProperty<EnumExchangePart> PART = EnumProperty.create("part", EnumExchangePart.class);
    /** True if the block on the start side (clockwise of the facing) is a heat exchanger facing the same way. */
    public static final BooleanProperty CONNECTED_LEFT = BooleanProperty.create("connected_left");
    /** True if the block on the end side (anticlockwise of the facing) is a heat exchanger facing the same way. */
    public static final BooleanProperty CONNECTED_RIGHT = BooleanProperty.create("connected_right");

    public BlockHeatExchange(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
            .setValue(PART, EnumExchangePart.MIDDLE).setValue(CONNECTED_LEFT, false).setValue(CONNECTED_RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, CONNECTED_LEFT, CONNECTED_RIGHT);
    }

    /** @return How far (in degrees, clockwise seen from above) the model is turned: it faces west unturned. */
    public static int modelRotation(Direction facing) {
        return BlockDistiller.modelRotation(facing);
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
        return direction.getAxis().isHorizontal() ? updateConnections(state, level, pos) : state;
    }

    /** @return The given state, connected to the heat exchangers on either side that face the same way. */
    public static BlockState updateConnections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state
            .setValue(CONNECTED_LEFT, connects(level, pos.relative(facing.getClockWise()), facing))
            .setValue(CONNECTED_RIGHT, connects(level, pos.relative(facing.getCounterClockWise()), facing));
    }

    private static boolean connects(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState neighbour = level.getBlockState(pos);
        return neighbour.getBlock() instanceof BlockHeatExchange && neighbour.getValue(FACING) == facing;
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileHeatExchange(pos, state);
    }

    @Override
    public InteractionResult attemptRotation(Level level, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (level.getBlockEntity(pos) instanceof TileHeatExchange exchange) {
            return exchange.rotate() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public float getExtension(Level level, BlockPos pos, Direction face, BlockState state) {
        return 0;
    }
}
