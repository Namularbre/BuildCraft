/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PluggableDefinition;

import buildcraft.BuildCraft;
import buildcraft.silicon.plug.PluggableGate;

/** The pluggables of the silicon module. */
public final class BCSiliconPlugs {
    public static final PluggableDefinition GATE;

    static {
        // The registry is set up by the transport module, which is loaded first
        GATE = new PluggableDefinition(ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "gate"),
            PluggableGate::new, PluggableGate::new);
        PipeApi.pluggableRegistry.register(GATE);
    }

    private BCSiliconPlugs() {}

    /** Loads this class, so that every pluggable is defined. */
    static void init() {}
}
