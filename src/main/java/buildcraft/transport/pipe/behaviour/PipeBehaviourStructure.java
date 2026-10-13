/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import net.minecraft.nbt.CompoundTag;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;

/** Structure pipes don't do anything: they are for building (and crafting pluggables). */
public class PipeBehaviourStructure extends PipeBehaviour {
    public PipeBehaviourStructure(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourStructure(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }
}
