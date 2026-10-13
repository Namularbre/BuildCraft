/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import buildcraft.BuildCraft;
import buildcraft.silicon.plug.PluggableLens.KeyPlugLens;
import buildcraft.silicon.plug.PluggableLightSensor.KeyPlugLightSensor;
import buildcraft.silicon.plug.PluggablePulsar.KeyPlugPulsar;
import buildcraft.transport.client.model.PlugModels;

/** The models of the pulsar, light sensor and lens (on the west side). */
public final class SiliconPlugModels {
    private SiliconPlugModels() {}

    public static void register() {
        PlugModels.register(KeyPlugLightSensor.class, List.of(
            new PlugModels.Box(new float[] { 2, 5, 5 }, new float[] { 4.01f, 11, 11 }, texture("daylight_sensor"))
        ));
        PlugModels.registerDynamic(KeyPlugPulsar.class, SiliconPlugModels::pulsar);
        PlugModels.registerDynamic(KeyPlugLens.class, SiliconPlugModels::lens);
    }

    private static List<PlugModels.Box> pulsar(KeyPlugPulsar key) {
        return List.of(
            new PlugModels.Box(new float[] { 2, 5, 5 }, new float[] { 4.01f, 11, 11 }, texture("pulsar_static")),
            new PlugModels.Box(new float[] { 1.5f, 6, 6 }, new float[] { 4.5f, 10, 10 },
                texture(key.on ? "pulsar_dynamic_on" : "pulsar_dynamic_off"))
        );
    }

    private static List<PlugModels.Box> lens(KeyPlugLens key) {
        ResourceLocation frame = texture(key.isFilter ? "filter" : "lens");
        List<PlugModels.Box> boxes = new ArrayList<>();
        boxes.add(new PlugModels.Box(new float[] { 0, 3, 3 }, new float[] { 2, 4.01f, 13 }, frame));
        boxes.add(new PlugModels.Box(new float[] { 0, 11.99f, 3 }, new float[] { 2, 13, 13 }, frame));
        boxes.add(new PlugModels.Box(new float[] { 0, 4.01f, 3 }, new float[] { 2, 11.99f, 4.01f }, frame));
        boxes.add(new PlugModels.Box(new float[] { 0, 4.01f, 11.99f }, new float[] { 2, 11.99f, 13 }, frame));
        if (key.isFilter) {
            boxes.add(new PlugModels.Box(new float[] { 0, 4.01f, 6 }, new float[] { 2, 11.99f, 7.01f }, frame));
            boxes.add(new PlugModels.Box(new float[] { 0, 4.01f, 9 }, new float[] { 2, 11.99f, 10.01f }, frame));
        }
        String glass = key.colour == null ? "glass" : key.colour.getName() + "_stained_glass";
        boxes.add(new PlugModels.Box(new float[] { 0.5f, 4.01f, 4.01f }, new float[] { 1.5f, 11.99f, 11.99f },
            ResourceLocation.withDefaultNamespace("block/" + glass)));
        return boxes;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/plug/" + name);
    }
}
