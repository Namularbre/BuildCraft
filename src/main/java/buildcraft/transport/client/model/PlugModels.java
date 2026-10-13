/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pluggable.PluggableModelKey;

/** The models of pluggables: boxes, as they are on the west side of the pipe (they are turned to the right side). */
public final class PlugModels {
    /** A box (in pixels, on the west side) with a single texture. */
    public record Box(float[] from, float[] to, ResourceLocation texture) {}

    private static final Map<Class<? extends PluggableModelKey>, Function<PluggableModelKey, List<Box>>> MODELS =
        new HashMap<>();

    private PlugModels() {}

    public static void register(Class<? extends PluggableModelKey> keyClass, List<Box> boxes) {
        MODELS.put(keyClass, key -> boxes);
    }

    /** Registers a model that depends on the data of the key (the result is cached per key). */
    public static <K extends PluggableModelKey> void registerDynamic(Class<K> keyClass, Function<K, List<Box>> model) {
        MODELS.put(keyClass, key -> model.apply(keyClass.cast(key)));
    }

    @Nullable
    public static List<Box> get(PluggableModelKey key) {
        Function<PluggableModelKey, List<Box>> model = MODELS.get(key.getClass());
        return model == null ? null : model.apply(key);
    }
}
