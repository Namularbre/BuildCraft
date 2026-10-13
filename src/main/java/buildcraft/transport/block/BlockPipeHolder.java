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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PipePluggable;

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
        VoxelShape shape = SHAPES[mask];
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            for (Direction dir : Direction.values()) {
                PipePluggable plug = tile.getPluggable(dir);
                if (plug != null) {
                    shape = Shapes.or(shape, Shapes.create(plug.getBoundingBox()));
                }
            }
        }
        return shape;
    }

    /** @return The side of the pluggable that the given (world) position is on, or null if it isn't on one. */
    @Nullable
    private static Direction getPluggableAt(TilePipeHolder tile, BlockPos pos, Vec3 location) {
        Vec3 local = location.subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Direction dir : Direction.values()) {
            PipePluggable plug = tile.getPluggable(dir);
            if (plug != null && plug.getBoundingBox().inflate(1e-4).contains(local)) {
                return dir;
            }
        }
        return null;
    }

    /** @return The side of the pluggable that the player is looking at, if it is closer than the pipe itself. */
    @Nullable
    private static Direction getPluggableLookedAt(TilePipeHolder tile, BlockPos pos, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1).scale(player.getBlockReach() + 1));
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction dir : Direction.values()) {
            PipePluggable plug = tile.getPluggable(dir);
            if (plug != null) {
                var hit = plug.getBoundingBox().move(pos).clip(eye, end);
                if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDist) {
                    bestDist = hit.get().distanceToSqr(eye);
                    best = dir;
                }
            }
        }
        if (best == null) {
            return null;
        }
        Pipe pipe = tile.getPipe();
        if (pipe != null) {
            var pipeHit = SHAPES[0].bounds().move(pos).clip(eye, end);
            if (pipeHit.isPresent() && pipeHit.get().distanceToSqr(eye) < bestDist) {
                return null;
            }
        }
        return best;
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
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            Direction plugSide = getPluggableAt(tile, pos, hit.getLocation());
            if (plugSide != null) {
                PipePluggable plug = tile.getPluggable(plugSide);
                return plug.onPluggableActivate(player, hit, (float) local.x, (float) local.y, (float) local.z)
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof IItemPluggable itemPlug) {
                Direction side = part.face != null ? part.face : hit.getDirection();
                if (tile.getPluggable(side) == null) {
                    PipePluggable plug = itemPlug.onPlace(held, tile, side, player, hand);
                    if (plug != null) {
                        tile.replacePluggable(side, plug);
                        plug.onPlacedBy(player);
                        if (!player.getAbilities().instabuild) {
                            held.shrink(1);
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
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

    /** Breaking a pluggable only removes it (not the pipe). */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
        FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            Direction plugSide = getPluggableLookedAt(tile, pos, player);
            if (plugSide != null) {
                if (!level.isClientSide) {
                    PipePluggable removed = tile.replacePluggable(plugSide, null);
                    if (removed != null) {
                        removed.onRemove();
                        if (!player.getAbilities().instabuild) {
                            NonNullList<ItemStack> drops = NonNullList.create();
                            removed.addDrops(drops);
                            drops.forEach(stack -> Block.popResource(level, pos, stack));
                        }
                    }
                }
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
        Player player) {
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            Direction plugSide = getPluggableAt(tile, pos, target.getLocation());
            if (plugSide != null) {
                return tile.getPluggable(plugSide).getPickStack();
            }
        }
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
