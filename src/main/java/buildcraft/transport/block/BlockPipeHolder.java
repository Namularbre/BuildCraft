/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.tile.TileBC;

import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.tile.TilePipeHolder;

/** The single block used by every pipe. The pipe itself lives in the {@link TilePipeHolder}. */
public class BlockPipeHolder extends BlockBCTile {
    private static final double MIN = 0.25;
    private static final double MAX = 0.75;
    private static final VoxelShape SHAPE_CENTER = Shapes.box(MIN, MIN, MIN, MAX, MAX, MAX);
    /** The shape of the pipe for every combination of connected sides (bit n set = connected on direction n). */
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = SHAPE_CENTER;
            for (Direction dir : Direction.values()) {
                if ((mask & (1 << dir.ordinal())) != 0) {
                    shape = Shapes.or(shape, getArmShape(dir));
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public BlockPipeHolder(Properties properties) {
        super(properties.noOcclusion().dynamicShape());
    }

    private static VoxelShape getArmShape(Direction dir) {
        return switch (dir) {
            case DOWN -> Shapes.box(MIN, 0, MIN, MAX, MIN, MAX);
            case UP -> Shapes.box(MIN, MAX, MIN, MAX, 1, MAX);
            case NORTH -> Shapes.box(MIN, MIN, 0, MAX, MAX, MIN);
            case SOUTH -> Shapes.box(MIN, MIN, MAX, MAX, MAX, 1);
            case WEST -> Shapes.box(0, MIN, MIN, MIN, MAX, MAX);
            case EAST -> Shapes.box(MAX, MIN, MIN, 1, MAX, MAX);
        };
    }

    @Override
    public TileBC newBlockEntity(BlockPos pos, BlockState state) {
        return new TilePipeHolder(pos, state);
    }

    @Nullable
    public static Pipe getPipe(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            return tile.getPipe();
        }
        return null;
    }

    // Shapes

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Pipe pipe = getPipe(level, pos);
        if (pipe == null) {
            return SHAPE_CENTER;
        }
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (pipe.isConnected(dir)) {
                mask |= 1 << dir.ordinal();
            }
        }
        return SHAPES[mask];
    }

    /** @return The part of the pipe that the given hit is on: the center, or one of the connections. */
    public static EnumPipePart getPartHit(BlockPos pos, Vec3 location) {
        Vec3 local = location.subtract(pos.getX(), pos.getY(), pos.getZ());
        // Arms are outside of the center cube, along a single axis
        final double epsilon = 1e-4;
        if (local.y < MIN - epsilon) return EnumPipePart.DOWN;
        if (local.y > MAX + epsilon) return EnumPipePart.UP;
        if (local.z < MIN - epsilon) return EnumPipePart.NORTH;
        if (local.z > MAX + epsilon) return EnumPipePart.SOUTH;
        if (local.x < MIN - epsilon) return EnumPipePart.WEST;
        if (local.x > MAX + epsilon) return EnumPipePart.EAST;
        return EnumPipePart.CENTER;
    }

    // Interaction

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hit) {
        Pipe pipe = getPipe(level, pos);
        if (pipe == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            // Let the server decide; only swing if the player is holding something the pipe might react to
            return player.getItemInHand(hand).isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        EnumPipePart part = getPartHit(pos, hit.getLocation());
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        if (pipe.getBehaviour().onPipeActivate(
            player, hit, (float) local.x, (float) local.y, (float) local.z, part
        )) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        Pipe pipe = getPipe(level, pos);
        if (pipe != null) {
            pipe.getBehaviour().onEntityCollide(entity);
        }
    }

    // Drops

    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        NonNullList<ItemStack> drops = NonNullList.create();
        if (tile instanceof TilePipeHolder holder) {
            holder.addDrops(drops);
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
        Player player) {
        Pipe pipe = getPipe(level, pos);
        if (pipe != null) {
            Item item = (Item) PipeApi.pipeRegistry.getItemForPipe(pipe.getDefinition());
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos,
        @Nullable Direction direction) {
        return false;
    }
}
