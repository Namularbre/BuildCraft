/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.util.EnumSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.items.ItemHandlerHelper;

/** An item moving through a pipe: either from one side to the center, or from the center out of a side. */
public class TravellingItem {
    /** The stack. On the client this is only used for rendering. */
    @Nonnull
    ItemStack stack;
    public DyeColor colour;
    boolean toCenter;
    double speed = 0.05;
    /** Absolute times (relative to world.getGameTime()) with when an item started to when it finishes. */
    long tickStarted, tickFinished;
    /** Relative times (from tickStarted) until an event needs to be fired or this item needs changing. */
    int timeToDest;
    /** If {@link #toCenter} is true then this represents the side that the item is coming from, otherwise this
     * represents the side that the item is going to. */
    Direction side;
    /** A set of all the faces that this item has tried to go and failed. */
    EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);
    /** If true then events won't be fired for this, and this item won't be dropped by the pipe. However it will affect
     * pipe.isEmpty and related gate triggers. */
    boolean isPhantom = false;

    // @formatter:off
    /* States (server side):

      - TO_CENTER:
        - tickStarted is the tick that the item entered the pipe (or bounced back)
        - tickFinished is the tick that the item will reach the center
        - side is the side that the item came from
        - timeToDest is equal to timeFinished - timeStarted

      - TO_EXIT:
       - tickStarted is the tick that the item reached the center
       - tickFinished is the tick that the item will reach the end of a pipe
       - side is the side that the item is going to
       - timeToDest is equal to timeFinished - timeStarted.
     */
    // @formatter:on

    public TravellingItem(@Nonnull ItemStack stack) {
        this.stack = stack;
    }

    public TravellingItem(CompoundTag nbt, long tickNow) {
        stack = ItemStack.of(nbt.getCompound("stack"));
        int c = nbt.getByte("colour");
        this.colour = c == 0 ? null : DyeColor.byId(c - 1);
        this.toCenter = nbt.getBoolean("toCenter");
        this.speed = nbt.getDouble("speed");
        if (speed < 0.001) {
            // Just to make sure that we don't have an invalid speed
            speed = 0.001;
        }
        tickStarted = nbt.getInt("tickStarted") + tickNow;
        tickFinished = nbt.getInt("tickFinished") + tickNow;
        timeToDest = nbt.getInt("timeToDest");

        side = Direction.byName(nbt.getString("side"));
        if (side == null || timeToDest == 0) {
            toCenter = true;
        }
        int triedBits = nbt.getByte("tried");
        for (Direction face : Direction.values()) {
            if ((triedBits & (1 << face.ordinal())) != 0) {
                tried.add(face);
            }
        }
        isPhantom = nbt.getBoolean("isPhantom");
    }

    public CompoundTag writeToNbt(long tickNow) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("stack", stack.save(new CompoundTag()));
        nbt.putByte("colour", (byte) (colour == null ? 0 : colour.getId() + 1));
        nbt.putBoolean("toCenter", toCenter);
        nbt.putDouble("speed", speed);
        nbt.putInt("tickStarted", (int) (tickStarted - tickNow));
        nbt.putInt("tickFinished", (int) (tickFinished - tickNow));
        nbt.putInt("timeToDest", timeToDest);
        if (side != null) {
            nbt.putString("side", side.getName());
        }
        int triedBits = 0;
        for (Direction face : tried) {
            triedBits |= 1 << face.ordinal();
        }
        nbt.putByte("tried", (byte) triedBits);
        if (isPhantom) {
            nbt.putBoolean("isPhantom", true);
        }
        return nbt;
    }

    @Nonnull
    public ItemStack getStack() {
        return stack;
    }

    public int getCurrentDelay(long tickNow) {
        long diff = tickFinished - tickNow;
        if (diff < 0) {
            return 0;
        } else {
            return (int) diff;
        }
    }

    public void genTimings(long now, double distance) {
        tickStarted = now;
        timeToDest = (int) Math.ceil(distance / speed);
        tickFinished = now + timeToDest;
    }

    public boolean canMerge(TravellingItem with) {
        if (isPhantom || with.isPhantom) {
            return false;
        }
        return toCenter == with.toCenter//
            && colour == with.colour//
            && side == with.side//
            && Math.abs(tickFinished - with.tickFinished) < 4//
            && stack.getMaxStackSize() >= stack.getCount() + with.stack.getCount()//
            && ItemHandlerHelper.canItemStacksStack(stack, with.stack);
    }

    /** Attempts to merge the two travelling item's together, if they are close enough. */
    public boolean mergeWith(TravellingItem with) {
        if (canMerge(with)) {
            this.stack.grow(with.stack.getCount());
            return true;
        }
        return false;
    }

    public Vec3 getRenderPosition(BlockPos pos, long tick, float partialTicks, PipeFlowItems flow) {
        long diff = tickFinished - tickStarted;
        long afterTick = tick - tickStarted;

        float interp = diff <= 0 ? 1 : (afterTick + partialTicks) / diff;
        interp = Math.max(0, Math.min(1, interp));

        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 vecSide = side == null ? center : center.relative(side, flow.getPipeLength(side));

        Vec3 vecFrom;
        Vec3 vecTo;
        if (toCenter) {
            vecFrom = vecSide;
            vecTo = center;
        } else {
            vecFrom = center;
            vecTo = vecSide;
        }

        return vecFrom.scale(1 - interp).add(vecTo.scale(interp));
    }

    @Nullable
    public Direction getRenderDirection() {
        if (toCenter) {
            return side == null ? null : side.getOpposite();
        } else {
            return side;
        }
    }
}
