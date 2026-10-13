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
import buildcraft.silicon.plug.PluggableFacade;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.plug.PluggableLens;
import buildcraft.silicon.plug.PluggableLightSensor;
import buildcraft.silicon.plug.PluggablePulsar;

/** The pluggables of the silicon module. */
public final class BCSiliconPlugs {
    public static final PluggableDefinition GATE;
    public static final PluggableDefinition PULSAR;
    public static final PluggableDefinition LIGHT_SENSOR;
    public static final PluggableDefinition LENS;
    public static final PluggableDefinition FACADE;

    static {
        // The registry is set up by the transport module, which is loaded first
        GATE = new PluggableDefinition(ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "gate"),
            PluggableGate::new, PluggableGate::new);
        PipeApi.pluggableRegistry.register(GATE);
        PULSAR = new PluggableDefinition(id("pulsar"), PluggablePulsar::new, PluggablePulsar::new);
        PipeApi.pluggableRegistry.register(PULSAR);
        LIGHT_SENSOR = new PluggableDefinition(id("light_sensor"), PluggableLightSensor::new);
        PipeApi.pluggableRegistry.register(LIGHT_SENSOR);
        LENS = new PluggableDefinition(id("lens"), PluggableLens::new, PluggableLens::new);
        PipeApi.pluggableRegistry.register(LENS);
        FACADE = new PluggableDefinition(id("facade"), PluggableFacade::new, PluggableFacade::new);
        PipeApi.pluggableRegistry.register(FACADE);
    }

    private BCSiliconPlugs() {}

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, name);
    }

    /** Loads this class, so that every pluggable is defined. */
    static void init() {}
}
