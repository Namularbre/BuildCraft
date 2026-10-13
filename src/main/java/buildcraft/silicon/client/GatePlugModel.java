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
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate.KeyPlugGate;
import buildcraft.transport.client.model.PlugModels;

/** The model of a gate on a pipe (on the west side), as in 1.12: the material, a lamp, the logic and the modifier. */
public final class GatePlugModel {
    private GatePlugModel() {}

    public static List<PlugModels.Box> boxes(KeyPlugGate key) {
        GateVariant variant = key.variant;
        List<PlugModels.Box> boxes = new ArrayList<>();
        boxes.add(new PlugModels.Box(new float[] { 2, 5, 5 }, new float[] { 4.01f, 11, 11 },
            ResourceLocation.withDefaultNamespace("block/" + variant.material().texture)));
        boxes.add(new PlugModels.Box(new float[] { 1.9f, 6, 6 }, new float[] { 4.1f, 10, 10 },
            gateTexture(key.on ? "gate_on" : "gate_off")));
        if (variant.material().canBeModified) {
            boxes.add(new PlugModels.Box(new float[] { 1.8f, 7, 7 }, new float[] { 4.2f, 9, 9 },
                gateTexture("gate_" + variant.logic().tag)));
        }
        if (variant.modifier() != EnumGateModifier.NO_MODIFIER) {
            ResourceLocation texture = ResourceLocation.withDefaultNamespace("block/" + variant.modifier().texture);
            for (float y : new float[] { 5.5f, 9.5f }) {
                for (float z : new float[] { 5.5f, 9.5f }) {
                    boxes.add(new PlugModels.Box(new float[] { 1.8f, y, z }, new float[] { 4.2f, y + 1, z + 1 },
                        texture));
                }
            }
        }
        return boxes;
    }

    private static ResourceLocation gateTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/gate/" + name);
    }
}
