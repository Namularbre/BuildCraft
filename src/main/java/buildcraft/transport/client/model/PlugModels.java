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

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pluggable.PluggableModelKey;

/** The models of pluggables: boxes, as they are on the west side of the pipe (they are turned to the right side). */
public final class PlugModels {
    /** Tint indexes of plug quads are offset by this (plus 100 times the side of the plug), so that the colour handler
     * of pipes knows which plug a quad belongs to. */
    public static final int TINT_OFFSET = 1000;

    /** A box (in pixels).
     *
     * @param texture The texture of every face, unless layers is given.
     * @param layers The textures (drawn on top of each other) of each face, given its direction in the world.
     * @param rotated True if the box is on the west side and has to be turned to the side of the plug, false if it is
     *            already where it should be.
     * @param translucent True if the box has to be drawn with the translucent blocks (like stained glass). */
    public record Box(float[] from, float[] to, ResourceLocation texture,
        @Nullable Function<Direction, List<Layer>> layers, boolean rotated, boolean translucent) {

        /** A box on the west side, with a single texture. */
        public Box(float[] from, float[] to, ResourceLocation texture) {
            this(from, to, texture, null, true, false);
        }

        public List<Layer> getLayers(Direction face) {
            return layers == null ? List.of(new Layer(texture, -1)) : layers.apply(face);
        }
    }

    /** A texture of a face, and its tint index (or -1 if it isn't tinted). */
    public record Layer(ResourceLocation texture, int tintIndex) {}

    /** @return The tint index of a quad of the plug on the given side, for its own tint index. */
    public static int encodeTint(Direction side, int tintIndex) {
        return TINT_OFFSET + side.ordinal() * 100 + tintIndex;
    }

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
