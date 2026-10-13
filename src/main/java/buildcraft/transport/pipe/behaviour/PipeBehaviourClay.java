/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventFluid;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;

/** Sends items and fluids into the inventories and tanks next to it first, and only into other pipes if they can't
 * go there. */
public class PipeBehaviourClay extends PipeBehaviour {
    /* We only really need to increase the priority, but using a larger number means that it doesn't matter what plugs
     * are attached (e.g. filters): this will always prefer to go into inventories above the other sides. (Although
     * the filters still matter.) */
    private static final int PRIORITY = 100;

    public PipeBehaviourClay(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourClay(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }

    @PipeEventHandler
    public void orderSides(PipeEventItem.SideCheck ordering) {
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) == ConnectedType.TILE) {
                ordering.increasePriority(face, PRIORITY);
            }
        }
    }

    @PipeEventHandler
    public void orderSides(PipeEventFluid.SideCheck ordering) {
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) == ConnectedType.TILE) {
                ordering.increasePriority(face, PRIORITY);
            }
        }
    }
}
