/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.plug;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.transport.BCTransportItems;

/** Closes a side of a pipe, but lets engines there power the pipe (if the pipe accepts power, like gates do). */
public class PluggablePowerAdaptor extends PipePluggable {
    private static final AABB[] BOXES = PlugBoxes.make(0, 3, 4);

    public PluggablePowerAdaptor(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCTransportItems.PLUG_POWER_ADAPTOR.get());
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if (cap == MjAPI.CAP_CONNECTOR || cap == MjAPI.CAP_RECEIVER || cap == MjAPI.CAP_REDSTONE_RECEIVER) {
            IPipe pipe = holder.getPipe();
            if (pipe != null) {
                return pipe.getBehaviour().getCapability(cap, side);
            }
        }
        return LazyOptional.empty();
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new Key(side);
    }

    public static final class Key extends PluggableModelKey {
        public Key(Direction side) {
            super(side);
        }
    }
}
