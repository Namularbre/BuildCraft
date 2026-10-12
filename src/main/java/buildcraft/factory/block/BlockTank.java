/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.transport.pipe.ICustomPipeConnection;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileTank;

public class BlockTank extends BlockBCTile implements ICustomPipeConnection {
    /** True if there is a tank below this one, so that the side texture joins it. */
    public static final BooleanProperty JOINED_BELOW = BooleanProperty.create("joined_below");
    private static final VoxelShape SHAPE = Shapes.box(2 / 16D, 0, 2 / 16D, 14 / 16D, 1, 14 / 16D);

    public BlockTank(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(JOINED_BELOW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINED_BELOW);
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileTank(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState().setValue(JOINED_BELOW, below.getBlock() instanceof BlockTank);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
        BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(JOINED_BELOW, neighbour.getBlock() instanceof BlockTank);
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Tanks in a column look like a single tank: the faces between them aren't drawn. */
    @Override
    @SuppressWarnings("deprecation")
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return direction.getAxis() == Direction.Axis.Y && adjacent.getBlock() instanceof BlockTank;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileTank tank ? tank.getComparatorLevel() : 0;
    }

    @Override
    public float getExtension(Level level, BlockPos pos, Direction face, BlockState state) {
        return face.getAxis() == Direction.Axis.Y ? 0 : 2 / 16f;
    }
}
