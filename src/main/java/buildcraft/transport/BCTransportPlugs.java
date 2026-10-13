/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableDefinition.IPluggableCreator;

import buildcraft.lib.registry.PluggableRegistry;

import buildcraft.BuildCraft;
import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.plug.PluggablePowerAdaptor;

/** The pluggables of the transport module. */
public final class BCTransportPlugs {
    static {
        PipeApi.pluggableRegistry = PluggableRegistry.INSTANCE;
    }

    public static final PluggableDefinition BLOCKER = register("blocker", PluggableBlocker::new);
    public static final PluggableDefinition POWER_ADAPTOR = register("power_adaptor", PluggablePowerAdaptor::new);

    private BCTransportPlugs() {}

    public static PluggableDefinition register(String name, IPluggableCreator creator) {
        PluggableDefinition definition =
            new PluggableDefinition(ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, name), creator);
        PipeApi.pluggableRegistry.register(definition);
        return definition;
    }

    /** Loads this class, so that every pluggable is defined. */
    static void init() {}
}
