/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.tile.TileAutoWorkbenchItems;

public class BlockAutoWorkbenchItems extends BlockBCTile {
    public BlockAutoWorkbenchItems(Properties properties) {
        super(properties);
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TileAutoWorkbenchItems(pos, state);
    }
}
