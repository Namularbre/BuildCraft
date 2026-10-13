/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.facades.IFacadeState;

/** A block state that facades can look like, and the item that makes them. */
public class FacadeBlockStateInfo implements IFacadeState {
    public final BlockState state;
    public final ItemStack requiredStack;
    public final boolean isTransparent;
    /** True if this can be made (so shown in creative and recipes). */
    public final boolean isVisible;

    /** @param isVisible True if this can be made (if false, it is only kept so that existing facades still load). */
    public FacadeBlockStateInfo(BlockState state, ItemStack requiredStack, boolean isVisible) {
        this.state = Objects.requireNonNull(state, "state must not be null!");
        this.requiredStack = requiredStack;
        this.isTransparent = !state.canOcclude();
        this.isVisible = isVisible && !requiredStack.isEmpty();
    }

    public FacadePhasedState createPhased(@Nullable DyeColor activeColour) {
        return new FacadePhasedState(this, activeColour);
    }

    @Override
    public String toString() {
        return "StateInfo[" + state + "]";
    }

    @Override
    public BlockState getBlockState() {
        return state;
    }

    @Override
    public boolean isTransparent() {
        return isTransparent;
    }

    @Override
    public ItemStack getRequiredStack() {
        return requiredStack;
    }
}
