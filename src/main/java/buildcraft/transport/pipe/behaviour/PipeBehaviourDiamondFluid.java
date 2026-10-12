/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;

/** Sends fluids to the sides whose filters (fluid containers, like buckets) hold the same fluid. A side without
 * filters takes what no other side wants. */
public class PipeBehaviourDiamondFluid extends PipeBehaviourDiamond {
    public PipeBehaviourDiamondFluid(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDiamondFluid(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventFluid.SideCheck sideCheck) {
        FluidStack toCompare = sideCheck.fluid;
        for (Direction face : Direction.values()) {
            if (!sideCheck.isAllowed(face) || !pipe.isConnected(face)) {
                continue;
            }
            int offset = FILTERS_PER_SIDE * face.ordinal();
            boolean hasFilter = false;
            boolean matched = false;
            for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                ItemStack filter = filters.getStackInSlot(offset + i);
                FluidStack target = FluidUtil.getFluidContained(filter).orElse(FluidStack.EMPTY);
                if (!target.isEmpty()) {
                    hasFilter = true;
                    if (target.isFluidEqual(toCompare)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                sideCheck.increasePriority(face, 12);
            } else if (hasFilter) {
                sideCheck.disallow(face);
            }
        }
    }
}
