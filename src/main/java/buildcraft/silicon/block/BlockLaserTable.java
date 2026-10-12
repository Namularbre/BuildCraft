/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.mj.ILaserTargetBlock;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

/** A table that lasers can power: assembly, advanced crafting, integration... */
public class BlockLaserTable extends BlockBCTile implements ILaserTargetBlock {
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 9 / 16D, 1);

    @FunctionalInterface
    public interface TileFactory {
        TileBC create(BlockPos pos, BlockState state);
    }

    private final TileFactory tileFactory;

    public BlockLaserTable(Properties properties, TileFactory tileFactory) {
        super(properties.noOcclusion());
        this.tileFactory = tileFactory;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return tileFactory.create(pos, state);
    }
}
