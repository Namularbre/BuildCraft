/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import buildcraft.api.facades.FacadeType;
import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadePhasedState;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.silicon.BCSiliconItems;

/** A thin slab that looks like a block, covering a whole side of a pipe. A hollow facade lets the pipe connect through
 * it; a full one doesn't. */
public class PluggableFacade extends PipePluggable implements IFacade {
    private static final AABB[] BOXES = new AABB[6];

    static {
        double ll = 0 / 16.0;
        double lu = 2 / 16.0;
        double ul = 14 / 16.0;
        double uu = 16 / 16.0;
        double min = 0 / 16.0;
        double max = 16 / 16.0;
        BOXES[Direction.DOWN.ordinal()] = new AABB(min, ll, min, max, lu, max);
        BOXES[Direction.UP.ordinal()] = new AABB(min, ul, min, max, uu, max);
        BOXES[Direction.NORTH.ordinal()] = new AABB(min, min, ll, max, max, lu);
        BOXES[Direction.SOUTH.ordinal()] = new AABB(min, min, ul, max, max, uu);
        BOXES[Direction.WEST.ordinal()] = new AABB(ll, min, min, lu, max, max);
        BOXES[Direction.EAST.ordinal()] = new AABB(ul, min, min, uu, max, max);
    }

    /** How thick facades are, in pixels. */
    public static final int SIZE = 2;

    public final FacadeInstance states;
    public int activeState;

    public PluggableFacade(PluggableDefinition definition, IPipeHolder holder, Direction side, FacadeInstance states) {
        super(definition, holder, side);
        this.states = states;
    }

    public PluggableFacade(PluggableDefinition def, IPipeHolder holder, Direction side, CompoundTag nbt) {
        super(def, holder, side);
        states = FacadeInstance.readFromNbt(nbt.getCompound("facade"));
        activeState = Mth.clamp(nbt.getInt("activeState"), 0, states.phasedStates.length - 1);
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("facade", states.writeToNbt());
        nbt.putInt("activeState", activeState);
        return nbt;
    }

    // Networking

    public PluggableFacade(PluggableDefinition def, IPipeHolder holder, Direction side, FriendlyByteBuf buffer) {
        super(def, holder, side);
        states = FacadeInstance.readFromBuffer(buffer);
        activeState = Mth.clamp(buffer.readUnsignedByte(), 0, states.phasedStates.length - 1);
    }

    @Override
    public void writeCreationPayload(FriendlyByteBuf buffer) {
        states.writeToBuffer(buffer);
        buffer.writeByte(activeState);
    }

    // Pluggable methods

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return !isHollow();
    }

    @Override
    public ItemStack getPickStack() {
        return BCSiliconItems.PLUG_FACADE.get().createItemStack(states);
    }

    /** @return The block state that the facade looks like right now. */
    public BlockState getActiveBlockState() {
        return states.phasedStates[activeState].stateInfo.state;
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        // The sides that already have a facade drawn over the corners shared with this one
        int trimmed = 0;
        for (Direction other : Direction.values()) {
            if (other.getAxis() != side.getAxis() && other.ordinal() < side.ordinal()
                && holder.getPluggable(other) instanceof PluggableFacade) {
                trimmed |= 1 << other.ordinal();
            }
        }
        return new KeyPlugFacade(side, getActiveBlockState(), isHollow(), trimmed);
    }

    // IFacade

    @Override
    public FacadeType getType() {
        return states.getType();
    }

    @Override
    public boolean isHollow() {
        return states.isHollow();
    }

    @Override
    public IFacadePhasedState[] getPhasedStates() {
        return states.getPhasedStates();
    }

    /** The model of a facade: the block it looks like, whether it is hollow, and which of its edges are covered by
     * facades on other sides (as a bit set of their {@link Direction#ordinal()}). */
    public static final class KeyPlugFacade extends PluggableModelKey {
        public final BlockState state;
        public final boolean isHollow;
        public final int trimmed;

        public KeyPlugFacade(Direction side, BlockState state, boolean isHollow, int trimmed) {
            super(side);
            this.state = state;
            this.isHollow = isHollow;
            this.trimmed = trimmed;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            KeyPlugFacade other = (KeyPlugFacade) obj;
            return other.state == state && other.isHollow == isHollow && other.trimmed == trimmed;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), state, isHollow, trimmed);
        }
    }
}
