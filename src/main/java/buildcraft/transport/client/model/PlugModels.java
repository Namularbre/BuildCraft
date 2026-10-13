/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pluggable.PluggableModelKey;

/** The models of pluggables: boxes, as they are on the west side of the pipe (they are turned to the right side). */
public final class PlugModels {
    /** A box (in pixels, on the west side) with a single texture. */
    public record Box(float[] from, float[] to, ResourceLocation texture) {}

    private static final Map<Class<? extends PluggableModelKey>, List<Box>> MODELS = new HashMap<>();

    private PlugModels() {}

    public static void register(Class<? extends PluggableModelKey> keyClass, List<Box> boxes) {
        MODELS.put(keyClass, boxes);
    }

    @Nullable
    public static List<Box> get(PluggableModelKey key) {
        return MODELS.get(key.getClass());
    }
}
