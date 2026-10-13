/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.facades.IFacadePhasedState;
import buildcraft.api.facades.IFacadeState;

/** One state of a facade, and the pipe signal colour that makes it active (null for the default state). */
public class FacadePhasedState implements IFacadePhasedState {
    public final FacadeBlockStateInfo stateInfo;
    @Nullable
    public final DyeColor activeColour;

    public FacadePhasedState(FacadeBlockStateInfo stateInfo, @Nullable DyeColor activeColour) {
        this.stateInfo = stateInfo;
        this.activeColour = activeColour;
    }

    public static FacadePhasedState readFromNbt(CompoundTag nbt) {
        FacadeBlockStateInfo stateInfo = FacadeStateManager.defaultState;
        if (nbt.contains("state")) {
            BlockState blockState =
                NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("state"));
            stateInfo = FacadeStateManager.getInfo(blockState);
        }
        DyeColor colour = nbt.contains("activeColour") ? DyeColor.byName(nbt.getString("activeColour"), null) : null;
        return new FacadePhasedState(stateInfo, colour);
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("state", NbtUtils.writeBlockState(stateInfo.state));
        if (activeColour != null) {
            nbt.putString("activeColour", activeColour.getName());
        }
        return nbt;
    }

    public static FacadePhasedState readFromBuffer(FriendlyByteBuf buf) {
        BlockState state = Block.stateById(buf.readVarInt());
        int colour = buf.readByte();
        return new FacadePhasedState(FacadeStateManager.getInfo(state), colour < 0 ? null : DyeColor.byId(colour));
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(Block.getId(stateInfo.state));
        buf.writeByte(activeColour == null ? -1 : activeColour.getId());
    }

    @Override
    public String toString() {
        return (activeColour == null ? "" : activeColour + " ") + stateInfo;
    }

    @Override
    public IFacadeState getState() {
        return stateInfo;
    }

    @Override
    public DyeColor getActiveColor() {
        return activeColour;
    }
}
