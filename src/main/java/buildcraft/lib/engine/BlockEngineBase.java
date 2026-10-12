/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.enums.EnumPowerStage;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

/** A single engine type. The direction the engine pushes power to, and its current heat stage, are part of the block
 * state so that the static parts of the engine can be a normal block model. */
public class BlockEngineBase extends BlockBCTile implements ICustomRotationHandler {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<EnumPowerStage> STAGE = EnumProperty.create("stage", EnumPowerStage.class);

    private final Supplier<? extends BlockEntityType<? extends TileEngineBase>> tileType;

    public BlockEngineBase(Properties properties,
        Supplier<? extends BlockEntityType<? extends TileEngineBase>> tileType) {
        super(properties.noOcclusion());
        this.tileType = tileType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(STAGE, EnumPowerStage.BLUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return tileType.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
        boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileEngineBase engine) {
            engine.rotateIfInvalid();
        }
    }

    // ICustomRotationHandler

    @Override
    public InteractionResult attemptRotation(Level level, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (level.getBlockEntity(pos) instanceof TileEngineBase engine) {
            return engine.attemptRotation();
        }
        return InteractionResult.FAIL;
    }
}
