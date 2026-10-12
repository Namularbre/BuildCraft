/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.RestrictedFluidHandler;
import buildcraft.lib.mj.MjRedstoneBatteryReceiver;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryConfig;

/** Pumps the fluid below the end of its tube, starting with the furthest blocks. */
public class TilePump extends TileMiner {
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    /** The power needed to pump one block. */
    public static final long POWER_PER_BLOCK = 10 * MjAPI.MJ;
    /** How many ticks to wait before looking for fluid again, when there wasn't any to pump. */
    private static final int REBUILD_DELAY = 30;

    private static final Direction[] SEARCH_NORMAL = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private static final Direction[] SEARCH_GASEOUS = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    /** The way from the end of the tube to a fluid block, through blocks of the same fluid. */
    private record FluidPath(BlockPos thisPos, @Nullable FluidPath parent) {}

    public final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private boolean queueBuilt = false;
    private final Map<BlockPos, FluidPath> paths = new HashMap<>();
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private boolean isInfiniteWaterSource;
    private long nextRebuild;

    /** The position just below the bottom of the pump tube. */
    @Nullable
    private BlockPos targetPos;

    public TilePump(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.PUMP_TILE.get(), pos, state);
        // Other blocks can only drain the pump
        caps.addCapabilityInstance(
            ForgeCapabilities.FLUID_HANDLER, RestrictedFluidHandler.drainOnly(tank), EnumPipePart.VALUES
        );
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjRedstoneBatteryReceiver(battery);
    }

    @Override
    protected long getBatteryCapacity() {
        return 50 * MjAPI.MJ;
    }

    public boolean isInfiniteWaterSource() {
        return isInfiniteWaterSource;
    }

    private static boolean isSameFluid(FluidState state, Fluid fluid) {
        return !state.isEmpty() && state.getType().isSame(fluid);
    }

    private void buildQueue() {
        queue.clear();
        paths.clear();
        isInfiniteWaterSource = false;
        Fluid queueFluid = null;
        Set<BlockPos> checked = new HashSet<>();
        List<BlockPos> nextPosesToCheck = new ArrayList<>();
        int maxDepth = BCFactoryConfig.miningMaxDepth();
        for (targetPos = worldPosition.below(); !level.isOutsideBuildHeight(targetPos); targetPos = targetPos.below()) {
            if (worldPosition.getY() - targetPos.getY() > maxDepth) {
                break;
            }
            FluidState fluidState = level.getFluidState(targetPos);
            if (!fluidState.isEmpty()) {
                queueFluid = fluidState.getType();
                nextPosesToCheck.add(targetPos);
                paths.put(targetPos, new FluidPath(targetPos, null));
                checked.add(targetPos);
                if (fluidState.isSource()) {
                    queue.add(targetPos);
                }
                break;
            }
            BlockState state = level.getBlockState(targetPos);
            if (!state.isAir() && !state.is(BCFactoryBlocks.TUBE.get())) {
                break;
            }
        }
        if (queueFluid == null) {
            return;
        }

        Direction[] directions = queueFluid.getFluidType().isLighterThanAir() ? SEARCH_GASEOUS : SEARCH_NORMAL;
        boolean isWater = !BCFactoryConfig.pumpsConsumeWater() && queueFluid.isSame(Fluids.WATER);
        int maxDistance = BCFactoryConfig.pumpMaxDistance();
        long maxDistanceSquared = (long) maxDistance * maxDistance;
        outer: while (!nextPosesToCheck.isEmpty()) {
            List<BlockPos> toCheck = new ArrayList<>(nextPosesToCheck);
            nextPosesToCheck.clear();
            for (BlockPos posToCheck : toCheck) {
                int count = 0;
                for (Direction side : directions) {
                    BlockPos offsetPos = posToCheck.relative(side);
                    if (offsetPos.distSqr(targetPos) > maxDistanceSquared) {
                        continue;
                    }
                    if (checked.add(offsetPos)) {
                        FluidState fluidAt = level.getFluidState(offsetPos);
                        if (isSameFluid(fluidAt, queueFluid)) {
                            paths.put(offsetPos, new FluidPath(offsetPos, paths.get(posToCheck)));
                            if (fluidAt.isSource()) {
                                queue.add(offsetPos);
                            }
                            nextPosesToCheck.add(offsetPos);
                            count++;
                        }
                    } else {
                        // We've already tested this block: it *must* be a valid water source
                        count++;
                    }
                }
                if (isWater && count >= 2) {
                    // Same check as vanilla water uses to make new source blocks
                    BlockState below = level.getBlockState(posToCheck.below());
                    FluidState fluidBelow = level.getFluidState(posToCheck.below());
                    @SuppressWarnings("deprecation")
                    boolean solid = below.isSolid();
                    if (solid || (fluidBelow.isSource() && fluidBelow.getType().isSame(Fluids.WATER))) {
                        isInfiniteWaterSource = true;
                        break outer;
                    }
                }
            }
        }
    }

    private boolean canDrain(BlockPos blockPos) {
        FluidStack fluid = FluidUtilBC.getSourceFluid(level, blockPos);
        return tank.isEmpty() ? !fluid.isEmpty() : fluid.isFluidEqual(tank.getFluid());
    }

    private void nextPos() {
        while (!queue.isEmpty()) {
            currentPos = queue.removeLast();
            if (canDrain(currentPos)) {
                updateLength();
                return;
            }
        }
        currentPos = null;
        updateLength();
    }

    @Override
    @Nullable
    protected BlockPos getTargetPos() {
        if (queue.isEmpty()) {
            return null;
        }
        return targetPos;
    }

    @Override
    public void tick() {
        if (!queueBuilt && !level.isClientSide) {
            buildQueue();
            queueBuilt = true;
        }

        super.tick();

        if (!level.isClientSide) {
            FluidUtilBC.pushFluidAround(level, worldPosition, tank);
        }
    }

    @Override
    protected void mine() {
        if (tank.getFluidAmount() > tank.getCapacity() / 2) {
            return;
        }
        if (currentPos != null && paths.containsKey(currentPos)) {
            progress += battery.extractPower(0, POWER_PER_BLOCK - progress);
            if (progress < POWER_PER_BLOCK) {
                return;
            }

            FluidStack drain = FluidUtilBC.getSourceFluid(level, currentPos);
            if (!drain.isEmpty() && getFirstInvalidPointOnPath(currentPos) == null && canDrain(currentPos)) {
                tank.fill(drain, IFluidHandler.FluidAction.EXECUTE);
                progress = 0;
                isInfiniteWaterSource &= drain.getFluid().isSame(Fluids.WATER);
                if (!isInfiniteWaterSource) {
                    FluidUtilBC.drainSourceBlock(level, currentPos);
                    paths.remove(currentPos);
                    nextPos();
                }
                return;
            }
            // The fluid changed since the queue was built
            if (level.getGameTime() < nextRebuild) {
                return;
            }
        } else if (currentPos == null && level.getGameTime() < nextRebuild) {
            return;
        }
        nextRebuild = level.getGameTime() + REBUILD_DELAY;
        buildQueue();
        nextPos();
    }

    /** @return The first block on the way to the given position that doesn't hold fluid anymore, or null if they all
     *         do. */
    @Nullable
    private BlockPos getFirstInvalidPointOnPath(BlockPos from) {
        FluidPath path = paths.get(from);
        if (path == null) {
            return from;
        }
        do {
            if (level.getFluidState(path.thisPos()).isEmpty()) {
                return path.thisPos();
            }
        } while ((path = path.parent()) != null);
        return null;
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        tank.readFromNBT(nbt.getCompound("tank"));
    }
}
