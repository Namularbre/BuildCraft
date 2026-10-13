/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.data.ModelData;

import buildcraft.silicon.plug.PluggableFacade.KeyPlugFacade;
import buildcraft.transport.client.model.PlugModels;

/** The model of facades: a slab (or a frame, if hollow) with the textures of the block it looks like. */
public final class FacadeModels {
    /** The size of the hole of hollow facades: the same as the pipe. */
    private static final float HOLE_MIN = 4, HOLE_MAX = 12;
    private static final float THICKNESS = 2;

    private static final Map<BlockState, Map<Direction, List<PlugModels.Layer>>> LAYERS = new ConcurrentHashMap<>();

    private FacadeModels() {}

    public static void register() {
        PlugModels.registerDynamic(KeyPlugFacade.class, FacadeModels::boxes);
    }

    /** Forgets the textures read from block models (they change when the resources are reloaded). */
    public static void clearCache() {
        LAYERS.clear();
    }

    /** @return The textures of the faces of the block model of the given state, for each direction. */
    public static List<PlugModels.Layer> getLayers(BlockState state, Direction face) {
        return LAYERS.computeIfAbsent(state, FacadeModels::readLayers).getOrDefault(face, List.of());
    }

    private static Map<Direction, List<PlugModels.Layer>> readLayers(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        Map<Direction, List<PlugModels.Layer>> layers = new EnumMap<>(Direction.class);
        RandomSource rand = RandomSource.create(42);
        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = new ArrayList<>();
            for (RenderType type : model.getRenderTypes(state, rand, ModelData.EMPTY)) {
                rand.setSeed(42);
                quads.addAll(model.getQuads(state, face, rand, ModelData.EMPTY, type));
                rand.setSeed(42);
                for (BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, type)) {
                    if (quad.getDirection() == face) {
                        quads.add(quad);
                    }
                }
            }
            List<PlugModels.Layer> list = new ArrayList<>();
            for (BakedQuad quad : quads) {
                list.add(new PlugModels.Layer(quad.getSprite().contents().name(), quad.getTintIndex()));
            }
            layers.put(face, list);
        }
        return layers;
    }

    /** @return True if the block is drawn with the translucent blocks (like stained glass and ice). */
    public static boolean isTranslucent(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        return model.getRenderTypes(state, RandomSource.create(42), ModelData.EMPTY).contains(RenderType.translucent());
    }

    /** The boxes of a facade, in the block's coordinates (not turned from the west side). */
    private static List<PlugModels.Box> boxes(KeyPlugFacade key) {
        Direction side = key.side;
        float[] min = { 0, 0, 0 };
        float[] max = { 16, 16, 16 };
        int axis = side.getAxis().ordinal();
        if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = 16 - THICKNESS;
        } else {
            max[axis] = THICKNESS;
        }
        // The corners shared with facades on other sides are drawn by those facades
        for (Direction other : Direction.values()) {
            if ((key.trimmed & (1 << other.ordinal())) != 0) {
                int otherAxis = other.getAxis().ordinal();
                if (other.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    max[otherAxis] = 16 - THICKNESS;
                } else {
                    min[otherAxis] = THICKNESS;
                }
            }
        }
        List<float[][]> parts = new ArrayList<>();
        if (!key.isHollow) {
            parts.add(new float[][] { min, max });
        } else {
            // A frame around the hole, made of 4 boxes
            int a = (axis + 1) % 3;
            int b = (axis + 2) % 3;
            parts.add(part(min, max, a, min[a], HOLE_MIN, b, min[b], max[b]));
            parts.add(part(min, max, a, HOLE_MAX, max[a], b, min[b], max[b]));
            parts.add(part(min, max, a, HOLE_MIN, HOLE_MAX, b, min[b], HOLE_MIN));
            parts.add(part(min, max, a, HOLE_MIN, HOLE_MAX, b, HOLE_MAX, max[b]));
        }
        boolean translucent = isTranslucent(key.state);
        List<PlugModels.Box> boxes = new ArrayList<>();
        for (float[][] part : parts) {
            boxes.add(new PlugModels.Box(part[0], part[1], null, face -> getLayers(key.state, face), false,
                translucent));
        }
        return boxes;
    }

    private static float[][] part(float[] min, float[] max, int a, float minA, float maxA, int b, float minB,
        float maxB) {
        float[] from = min.clone();
        float[] to = max.clone();
        from[a] = minA;
        to[a] = maxA;
        from[b] = minB;
        to[b] = maxB;
        return new float[][] { from, to };
    }
}
