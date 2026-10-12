/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.ILaserTargetBlock;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;

import buildcraft.lib.misc.data.AverageLong;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.block.BlockLaser;

/** Sends the MJ it is given to the laser tables in front of it (in a cone up to 6 blocks away), picking a random one
 * that needs power every so often. */
public class TileLaser extends TileBC implements ITickable {
    private static final int TARGETING_RANGE = 6;
    /** Ticks between two looks for tables, as blocks around might have changed. */
    private static final int SCAN_INTERVAL = 20;
    private static final int TARGET_MOVE_INTERVAL = 10;
    public static final long MAX_POWER_PER_TICK = 4 * MjAPI.MJ;

    private final List<BlockPos> targetPositions = new ArrayList<>();
    @Nullable
    private BlockPos targetPos;
    /** Client side: where on the target the beam hits. */
    @Nullable
    private Vec3 laserPos;
    private int nextLaserMove;

    private final AverageLong avgPower = new AverageLong(100);
    private long averageClient;
    private final MjBattery battery = new MjBattery(1024 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));

    public TileLaser(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.LASER_TILE.get(), pos, state);
        caps.addProvider(mjCaps);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockLaser.FACING) ? state.getValue(BlockLaser.FACING) : Direction.UP;
    }

    /** Looks for laser tables in a square cone in front of the laser, that it can see. */
    private void findPossibleTargets() {
        targetPositions.clear();
        Direction facing = getFacing();
        Direction.Axis[] across = switch (facing.getAxis()) {
            case X -> new Direction.Axis[] { Direction.Axis.Y, Direction.Axis.Z };
            case Y -> new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Z };
            case Z -> new Direction.Axis[] { Direction.Axis.X, Direction.Axis.Y };
        };
        // Just outside of the laser itself, so that the ray doesn't hit it
        Vec3 from = Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.55));
        for (int d = 1; d <= TARGETING_RANGE; d++) {
            BlockPos centre = worldPosition.relative(facing, d);
            for (int i = -d; i <= d; i++) {
                for (int j = -d; j <= d; j++) {
                    BlockPos pos = centre.relative(across[0], i).relative(across[1], j);
                    if (isTarget(pos) && canSee(from, pos)) {
                        targetPositions.add(pos);
                    }
                }
            }
        }
    }

    private boolean isTarget(BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ILaserTargetBlock
            && level.getBlockEntity(pos) instanceof ILaserTarget;
    }

    /** @return True if nothing is between the laser and the given block. */
    private boolean canSee(Vec3 from, BlockPos target) {
        BlockHitResult hit = level.clip(new ClipContext(from, Vec3.atCenterOf(target), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    /** @return Where the beam starts: the end of the laser's barrel. */
    public Vec3 getEmitterPos() {
        return Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(getFacing().getNormal()).scale(4 / 16D));
    }

    private void randomlyChooseTargetPos() {
        List<BlockPos> needingPower = new ArrayList<>();
        for (BlockPos position : targetPositions) {
            if (isPowerNeededAt(position)) {
                needingPower.add(position);
            }
        }
        targetPos = needingPower.isEmpty() ? null : needingPower.get(level.random.nextInt(needingPower.size()));
    }

    private boolean isPowerNeededAt(@Nullable BlockPos position) {
        return position != null && level.getBlockEntity(position) instanceof ILaserTarget target
            && !target.isInvalidTarget() && target.getRequiredLaserPower() > 0;
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            // The beam moves around the table every few ticks
            if (--nextLaserMove <= 0 || (targetPos == null) != (laserPos == null)) {
                nextLaserMove = 5 + level.random.nextInt(6);
                laserPos = targetPos == null ? null : Vec3.atLowerCornerOf(targetPos).add(
                    (5 + level.random.nextInt(6) + 0.5) / 16D, 9 / 16D, (5 + level.random.nextInt(6) + 0.5) / 16D
                );
            }
            return;
        }

        avgPower.tick();
        BlockPos previousTarget = targetPos;
        long time = level.getGameTime();
        if (time % SCAN_INTERVAL == 0) {
            findPossibleTargets();
        }
        if (time % TARGET_MOVE_INTERVAL == 0 || !isPowerNeededAt(targetPos)) {
            randomlyChooseTargetPos();
        }

        if (targetPos != null && level.getBlockEntity(targetPos) instanceof ILaserTarget target) {
            // Sends less power when the battery is low
            long max = MAX_POWER_PER_TICK;
            max *= battery.getStored() + max;
            max /= battery.getCapacity() / 2;
            max = Math.min(Math.min(max, MAX_POWER_PER_TICK), target.getRequiredLaserPower());
            long power = battery.extractPower(0, max);
            long excess = target.receiveLaserPower(power);
            if (excess > 0) {
                battery.addPowerChecking(excess, false);
            }
            avgPower.push(power - excess);
        } else {
            avgPower.clear();
        }

        if (!Objects.equals(previousTarget, targetPos) || time % 10 == 0) {
            sendNetworkUpdate(NET_RENDER_DATA);
        }
        setChanged();
    }

    @Nullable
    public Vec3 getLaserPos() {
        return laserPos;
    }

    public long getAverageClient() {
        return averageClient;
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("battery", battery.serializeNBT());
        if (targetPos != null) {
            nbt.put("target_pos", NbtUtils.writeBlockPos(targetPos));
        }
        avgPower.writeToNbt(nbt, "average_power");
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        battery.deserializeNBT(nbt.getCompound("battery"));
        targetPos = nbt.contains("target_pos") ? NbtUtils.readBlockPos(nbt.getCompound("target_pos")) : null;
        avgPower.readFromNbt(nbt, "average_power");
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            battery.writeToBuffer(buffer);
            buffer.writeBoolean(targetPos != null);
            if (targetPos != null) {
                buffer.writeBlockPos(targetPos);
            }
            buffer.writeLong((long) avgPower.getAverage());
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_RENDER_DATA) {
            battery.readFromBuffer(buffer);
            targetPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
            averageClient = buffer.readLong();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = new AABB(worldPosition);
        return targetPos == null ? box : box.minmax(new AABB(targetPos));
    }
}
