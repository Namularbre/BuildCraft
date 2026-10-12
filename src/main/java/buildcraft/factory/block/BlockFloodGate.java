/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileFloodGate;

public class BlockFloodGate extends BlockBCTile {
    /** For each side but the top: true if the flood gate lets fluid out there. A wrench opens or closes a side. */
    public static final Map<Direction, BooleanProperty> OPEN = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            if (side != Direction.UP) {
                OPEN.put(side, PipeBlock.PROPERTY_BY_DIRECTION.get(side));
            }
        }
    }

    public BlockFloodGate(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty open : OPEN.values()) {
            state = state.setValue(open, true);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        OPEN.values().forEach(builder::add);
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileFloodGate(pos, state);
    }
}
