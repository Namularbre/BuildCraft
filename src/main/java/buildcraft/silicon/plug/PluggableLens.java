/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.silicon.BCSiliconItems;

/** A lens paints the items that go through it. A filter only lets items of its colour through (and prefers to send
 * them its way). */
public class PluggableLens extends PipePluggable {
    private static final AABB[] BOXES = new AABB[6];

    static {
        double ll = 0 / 16.0;
        double lu = 2 / 16.0;
        double ul = 14 / 16.0;
        double uu = 16 / 16.0;
        double min = 3 / 16.0;
        double max = 13 / 16.0;
        BOXES[Direction.DOWN.ordinal()] = new AABB(min, ll, min, max, lu, max);
        BOXES[Direction.UP.ordinal()] = new AABB(min, ul, min, max, uu, max);
        BOXES[Direction.NORTH.ordinal()] = new AABB(min, min, ll, max, max, lu);
        BOXES[Direction.SOUTH.ordinal()] = new AABB(min, min, ul, max, max, uu);
        BOXES[Direction.WEST.ordinal()] = new AABB(ll, min, min, lu, max, max);
        BOXES[Direction.EAST.ordinal()] = new AABB(ul, min, min, uu, max, max);
    }

    /** The colour, or null for a clear lens. */
    @Nullable
    public final DyeColor colour;
    public final boolean isFilter;

    // Manual constructor (called by the lens item)
    public PluggableLens(PluggableDefinition def, IPipeHolder holder, Direction side, @Nullable DyeColor colour,
        boolean isFilter) {
        super(def, holder, side);
        this.colour = colour;
        this.isFilter = isFilter;
    }

    // Saving + Loading
    public PluggableLens(PluggableDefinition def, IPipeHolder holder, Direction side, CompoundTag nbt) {
        super(def, holder, side);
        colour = nbt.contains("colour") ? DyeColor.byName(nbt.getString("colour"), null) : null;
        isFilter = nbt.getBoolean("f");
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        if (colour != null) {
            nbt.putString("colour", colour.getName());
        }
        nbt.putBoolean("f", isFilter);
        return nbt;
    }

    // Networking
    public PluggableLens(PluggableDefinition def, IPipeHolder holder, Direction side, FriendlyByteBuf buffer) {
        super(def, holder, side);
        int c = buffer.readByte();
        colour = c < 0 ? null : DyeColor.byId(c);
        isFilter = buffer.readBoolean();
    }

    @Override
    public void writeCreationPayload(FriendlyByteBuf buffer) {
        buffer.writeByte(colour == null ? -1 : colour.getId());
        buffer.writeBoolean(isFilter);
    }

    // Pluggable methods

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.lens(colour, isFilter));
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new KeyPlugLens(side, colour, isFilter);
    }

    @PipeEventHandler
    public void tryInsert(PipeEventItem.TryInsert tryInsert) {
        if (isFilter && tryInsert.from == side) {
            DyeColor itemColour = tryInsert.colour;
            if (itemColour != null && itemColour != colour) {
                tryInsert.cancel();
            }
        }
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck event) {
        if (isFilter) {
            if (event.colour == colour) {
                event.increasePriority(side);
            } else if (event.colour != null) {
                event.disallow(side);
            } else {
                event.decreasePriority(side);
            }
        }
    }

    @PipeEventHandler
    public void beforeInsert(PipeEventItem.OnInsert event) {
        if (!isFilter && event.from == side) {
            event.colour = colour;
        }
    }

    @PipeEventHandler
    public void reachEnd(PipeEventItem.ReachEnd event) {
        if (!isFilter && event.to == side) {
            event.colour = colour;
        }
    }

    public static final class KeyPlugLens extends PluggableModelKey {
        @Nullable
        public final DyeColor colour;
        public final boolean isFilter;

        public KeyPlugLens(Direction side, @Nullable DyeColor colour, boolean isFilter) {
            super(side);
            this.colour = colour;
            this.isFilter = isFilter;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && ((KeyPlugLens) obj).colour == colour
                && ((KeyPlugLens) obj).isFilter == isFilter;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), colour, isFilter);
        }
    }
}
