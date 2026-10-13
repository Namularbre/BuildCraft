/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Arrays;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.facades.FacadeType;
import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadePhasedState;

/** The states of a facade, and whether it is hollow. */
public class FacadeInstance implements IFacade {
    public final FacadePhasedState[] phasedStates;
    public final FacadeType type;
    public final boolean isHollow;

    public FacadeInstance(FacadePhasedState[] phasedStates, boolean isHollow) {
        if (phasedStates.length == 0) throw new IllegalArgumentException("phasedStates.length was 0");
        // Maximum of 17 states - 16 for each colour, 1 for no colour
        if (phasedStates.length > 17) throw new IllegalArgumentException("phasedStates.length was > 17");
        this.phasedStates = phasedStates;
        this.type = phasedStates.length == 1 ? FacadeType.Basic : FacadeType.Phased;
        this.isHollow = isHollow;
    }

    public static FacadeInstance createSingle(FacadeBlockStateInfo info, boolean isHollow) {
        return new FacadeInstance(new FacadePhasedState[] { new FacadePhasedState(info, null) }, isHollow);
    }

    public static FacadeInstance readFromNbt(CompoundTag nbt) {
        ListTag list = nbt.getList("states", Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return createSingle(FacadeStateManager.defaultState, false);
        }
        FacadePhasedState[] states = new FacadePhasedState[Math.min(17, list.size())];
        for (int i = 0; i < states.length; i++) {
            states[i] = FacadePhasedState.readFromNbt(list.getCompound(i));
        }
        return new FacadeInstance(states, nbt.getBoolean("isHollow"));
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (FacadePhasedState state : phasedStates) {
            list.add(state.writeToNbt());
        }
        nbt.put("states", list);
        nbt.putBoolean("isHollow", isHollow);
        return nbt;
    }

    public static FacadeInstance readFromBuffer(FriendlyByteBuf buf) {
        boolean isHollow = buf.readBoolean();
        int count = buf.readUnsignedByte();
        FacadePhasedState[] states = new FacadePhasedState[count];
        for (int i = 0; i < count; i++) {
            states[i] = FacadePhasedState.readFromBuffer(buf);
        }
        return new FacadeInstance(states, isHollow);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(isHollow);
        buf.writeByte(phasedStates.length);
        for (FacadePhasedState phasedState : phasedStates) {
            phasedState.writeToBuffer(buf);
        }
    }

    /** @return True if no state of this facade is already shown for the given colour (or by default, for null). */
    public boolean canAddColour(@Nullable DyeColor colour) {
        for (FacadePhasedState state : phasedStates) {
            if (state.activeColour == colour) {
                return false;
            }
        }
        return true;
    }

    /** @return This facade with one more state, or null if there already is one for the colour of the state. */
    @Nullable
    public FacadeInstance withState(FacadePhasedState state) {
        if (!canAddColour(state.activeColour) || phasedStates.length >= 17) {
            return null;
        }
        FacadePhasedState[] newStates = Arrays.copyOf(phasedStates, phasedStates.length + 1);
        newStates[phasedStates.length] = state;
        return new FacadeInstance(newStates, isHollow);
    }

    /** @return The state to show while the given wire colours are powered: the first state whose colour is powered,
     *         or else the default one. */
    public int getActiveState(Predicate<DyeColor> isPowered) {
        int defaultState = 0;
        for (int i = 0; i < phasedStates.length; i++) {
            DyeColor colour = phasedStates[i].activeColour;
            if (colour == null) {
                defaultState = i;
            } else if (isPowered.test(colour)) {
                return i;
            }
        }
        return defaultState;
    }

    /** @return The state to draw for an item: phased facades go through each of their states, one after the other. */
    public FacadePhasedState getCurrentStateForStack() {
        if (phasedStates.length == 1) {
            return phasedStates[0];
        }
        long now = System.currentTimeMillis() / 500;
        return phasedStates[(int) (now % phasedStates.length)];
    }

    public FacadeInstance withSwappedIsHollow() {
        return new FacadeInstance(phasedStates, !isHollow);
    }

    @Override
    public FacadeType getType() {
        return type;
    }

    @Override
    public boolean isHollow() {
        return isHollow;
    }

    @Override
    public IFacadePhasedState[] getPhasedStates() {
        return phasedStates;
    }
}
