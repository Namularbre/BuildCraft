/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.List;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Picks up the items dropped on its open side (the one opposite its only connection): those that touch it for free,
 * and those further away (up to 4 blocks) with power. */
public class PipeBehaviourObsidian extends PipeBehaviour implements IMjRedstoneReceiver {
    private static final long POWER_PER_ITEM = MjAPI.MJ / 2;
    private static final long POWER_PER_METRE = MjAPI.MJ / 4;

    private static final double INSERT_SPEED = 0.04;
    /** How long the items that this pipe dropped can't be picked up again. */
    private static final int DROP_GAP = 20;

    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(this);
    /** The items that were recently dropped, and the tick when they can be picked up. */
    private final WeakHashMap<ItemEntity, Long> entityDropTime = new WeakHashMap<>();
    private int toWaitTicks = 0;

    public PipeBehaviourObsidian(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourObsidian(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        // Saves us from writing out the ID of the item that was dropped
        toWaitTicks = DROP_GAP;
    }

    @Override
    public void onTick() {
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            return;
        }
        if (toWaitTicks > 0) {
            toWaitTicks--;
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        return !(other instanceof PipeBehaviourObsidian);
    }

    @Override
    public void onEntityCollide(Entity entity) {
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            return;
        }
        Direction openFace = getOpenFace();
        if (openFace != null) {
            trySuckEntity(entity, openFace, Long.MAX_VALUE, false);
        }
    }

    /** @return The side opposite the only connection of the pipe, or null if it doesn't have exactly one. */
    @Nullable
    private Direction getOpenFace() {
        Direction openFace = null;
        for (Direction face : Direction.values()) {
            if (pipe.isConnected(face)) {
                if (openFace != null) {
                    return null;
                }
                openFace = face.getOpposite();
            }
        }
        return openFace;
    }

    /** @return The area (distance blocks away from the open side) where items are picked up. */
    protected AABB getSuckingBox(Direction openFace, int distance) {
        Vec3 centre = Vec3.atCenterOf(pipe.getHolder().getPipePos());
        AABB box = new AABB(centre, centre).inflate(0.4)
            .move(openFace.getStepX() * distance, openFace.getStepY() * distance, openFace.getStepZ() * distance);
        double x = openFace.getAxis() == Direction.Axis.X ? 0.5 : distance;
        double y = openFace.getAxis() == Direction.Axis.Y ? 0.5 : distance;
        double z = openFace.getAxis() == Direction.Axis.Z ? 0.5 : distance;
        return box.inflate(x, y, z);
    }

    /** @return The power left over. */
    protected long trySuckEntity(Entity entity, Direction faceFrom, long power, boolean simulate) {
        if (!entity.isAlive() || !(entity instanceof ItemEntity item)) {
            return power;
        }
        Long tickPickup = entityDropTime.get(item);
        if (tickPickup != null) {
            if (pipe.getHolder().getPipeWorld().getGameTime() < tickPickup) {
                return power;
            }
            entityDropTime.remove(item);
        }
        if (!(pipe.getFlow() instanceof IFlowItems flow)) {
            return power;
        }
        ItemStack stack = item.getItem();
        if (stack.isEmpty()) {
            return power;
        }
        long powerPerItem;
        int max;
        if (power == Long.MAX_VALUE) {
            max = Integer.MAX_VALUE;
            powerPerItem = 0;
        } else {
            double distance = Math.sqrt(entity.distanceToSqr(Vec3.atCenterOf(pipe.getHolder().getPipePos())));
            powerPerItem = (long) (Math.max(1, distance) * POWER_PER_METRE + POWER_PER_ITEM);
            max = (int) (power / powerPerItem);
        }
        int count = Math.min(max, stack.getCount());
        if (count <= 0) {
            return power;
        }
        if (!simulate) {
            ItemStack extracted = stack.split(count);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            flow.insertItemsForce(extracted, faceFrom, null, INSERT_SPEED);
        }
        return power - powerPerItem * count;
    }

    @PipeEventHandler
    public void onPipeDrop(PipeEventItem.Drop drop) {
        entityDropTime.put(drop.getEntity(), pipe.getHolder().getPipeWorld().getGameTime() + DROP_GAP);
    }

    // IMjRedstoneReceiver

    @Override
    public boolean canConnect(@Nonnull IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        final long power = 512 * MjAPI.MJ;
        return power - receivePower(power, true);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (toWaitTicks > 0) {
            return microJoules;
        }
        Direction openFace = getOpenFace();
        if (openFace == null) {
            return microJoules;
        }
        for (int d = 1; d < 5; d++) {
            AABB box = getSuckingBox(openFace, d);
            List<Entity> entities = pipe.getHolder().getPipeWorld().getEntities((Entity) null, box, e -> true);
            for (Entity entity : entities) {
                long leftOver = trySuckEntity(entity, openFace, microJoules, simulate);
                if (leftOver < microJoules) {
                    return leftOver;
                }
            }
        }
        // Looking for items costs a little power (so that engines don't wait for nothing)
        return Math.max(0, microJoules - MjAPI.MJ);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return mjCaps.getCapability(capability, facing);
    }
}
