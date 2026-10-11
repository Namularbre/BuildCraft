/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

/** Base class for every BuildCraft block that has a {@link TileBC}. Forwards block events to the tile. */
public abstract class BlockBCTile extends BaseEntityBlock {
    public BlockBCTile(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public abstract TileBC newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE, as most block entities render themselves
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        return (lvl, pos, st, tile) -> {
            if (tile instanceof ITickable tickable) {
                tickable.tick();
            }
        };
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof TileBC tile) {
            tile.onExplode(explosion);
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TileBC tile) {
            tile.onRemove();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof TileBC tile) {
            tile.onPlacedBy(placer, stack);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TileBC tile) {
            InteractionResult result = tile.onActivated(player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
        boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.getBlockEntity(pos) instanceof TileBC tile) {
            tile.onNeighbourBlockChanged(block, fromPos);
        }
    }
}
