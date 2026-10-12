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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;

import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockFloodGate;

/** Places the fluid it is given as source blocks around it, through its open sides: liquids spread down and
 * sideways, and gases up and sideways. */
public class TileFloodGate extends TileBC implements ITickable {
    private static final Direction[] SEARCH_NORMAL = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final Direction[] SEARCH_GASEOUS = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    /** How long to wait before looking for space again, each time there wasn't any. */
    private static final int[] REBUILD_DELAYS = { 16, 32, 64, 128, 256 };
    /** Ticks between two placed blocks. */
    private static final int PLACE_DELAY = 16;
    private static final int MAX_DISTANCE = 64;
    private static final int MAX_QUEUE = 4096;

    public final FluidTank tank = new FluidTank(2 * FluidType.BUCKET_VOLUME) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** The positions to fill, nearest first. */
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    /** For each position to fill, the way to it from the flood gate. */
    private final Map<BlockPos, List<BlockPos>> paths = new HashMap<>();
    private int delayIndex = 0;
    private int tick = 0;

    public TileFloodGate(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.FLOOD_GATE_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.FLUID_HANDLER, tank, EnumPipePart.VALUES);
    }

    public boolean isOpen(Direction side) {
        BlockState state = getBlockState();
        return BlockFloodGate.OPEN.containsKey(side) && state.hasProperty(BlockFloodGate.OPEN.get(side))
            && state.getValue(BlockFloodGate.OPEN.get(side));
    }

    /** A wrench opens or closes the side it is used on (but the top is always closed). */
    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        Direction side = hit.getDirection();
        if (!(player.getItemInHand(hand).getItem() instanceof IToolWrench) || !BlockFloodGate.OPEN.containsKey(side)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockState state = getBlockState();
            level.setBlock(worldPosition, state.cycle(BlockFloodGate.OPEN.get(side)), Block.UPDATE_ALL);
            queue.clear();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void buildQueue() {
        queue.clear();
        paths.clear();
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        Set<BlockPos> checked = new HashSet<>();
        checked.add(worldPosition);
        List<BlockPos> nextPosesToCheck = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (isOpen(side)) {
                BlockPos offset = worldPosition.relative(side);
                nextPosesToCheck.add(offset);
                paths.put(offset, List.of(offset));
            }
        }
        Direction[] directions = fluid.getFluid().getFluidType().isLighterThanAir() ? SEARCH_GASEOUS : SEARCH_NORMAL;
        outer: while (!nextPosesToCheck.isEmpty()) {
            List<BlockPos> toCheckNow = new ArrayList<>(nextPosesToCheck);
            nextPosesToCheck.clear();
            for (BlockPos toCheck : toCheckNow) {
                if (toCheck.distSqr(worldPosition) > MAX_DISTANCE * MAX_DISTANCE || !checked.add(toCheck)
                    || !canSearch(toCheck)) {
                    continue;
                }
                if (canFill(toCheck)) {
                    queue.addLast(toCheck);
                    if (queue.size() >= MAX_QUEUE) {
                        break outer;
                    }
                }
                List<BlockPos> checkPath = paths.get(toCheck);
                for (Direction side : directions) {
                    BlockPos next = toCheck.relative(side);
                    if (!checked.contains(next)) {
                        List<BlockPos> path = new ArrayList<>(checkPath);
                        path.add(next);
                        paths.put(next, path);
                        nextPosesToCheck.add(next);
                    }
                }
            }
        }
    }

    /** @return True if a source block of the tank's fluid can be placed there: air, or the same fluid flowing. */
    private boolean canFill(BlockPos pos) {
        if (level.isEmptyBlock(pos)) {
            return true;
        }
        FluidState fluid = level.getFluidState(pos);
        return !fluid.isEmpty() && !fluid.isSource() && fluid.getType().isSame(tank.getFluid().getFluid());
    }

    /** @return True if the fluid can spread through there: somewhere to fill, or a source of the same fluid. */
    private boolean canSearch(BlockPos pos) {
        if (canFill(pos)) {
            return true;
        }
        FluidState fluid = level.getFluidState(pos);
        return fluid.isSource() && fluid.getType().isSame(tank.getFluid().getFluid());
    }

    /** @return True if there is (already) some of the tank's fluid there, so that the fluid can get through. */
    private boolean canFillThrough(BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return !fluid.isEmpty() && fluid.getType().isSame(tank.getFluid().getFluid());
    }

    @Override
    public void tick() {
        if (level.isClientSide || tank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }

        tick++;
        if (tick % PLACE_DELAY == 0 && !queue.isEmpty()) {
            FluidStack fluid = tank.drain(FluidType.BUCKET_VOLUME, FluidAction.SIMULATE);
            BlockPos currentPos = queue.removeFirst();
            if (canReach(currentPos) && canFill(currentPos)) {
                FakePlayer fakePlayer = FakePlayerFactory.get((ServerLevel) level, getOwner());
                if (FluidUtil.tryPlaceFluid(fakePlayer, level, InteractionHand.MAIN_HAND, currentPos, tank, fluid)) {
                    delayIndex = 0;
                    tick = 0;
                }
            } else {
                buildQueue();
            }
        }

        if (queue.isEmpty() && tick >= REBUILD_DELAYS[delayIndex]) {
            delayIndex = Math.min(delayIndex + 1, REBUILD_DELAYS.length - 1);
            tick = 0;
            buildQueue();
        }
    }

    /** @return True if the fluid has a way to the given position: every block on the way already holds some. */
    private boolean canReach(BlockPos target) {
        List<BlockPos> path = paths.get(target);
        if (path == null) {
            return true;
        }
        for (BlockPos pos : path) {
            if (!pos.equals(target) && !canFillThrough(pos)) {
                return false;
            }
        }
        return true;
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
