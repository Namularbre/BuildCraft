/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeFaceTex;

import buildcraft.transport.pipe.PipeModelKey;
import buildcraft.transport.tile.TilePipeHolder;

/** The model of every pipe, generated from the {@link PipeModelKey} that the pipe's block entity provides. Pipes are
 * see-through, so every face is also drawn from the inside. */
public class PipeBakedModel implements IDynamicBakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final float MIN = 4;
    private static final float MAX = 12;
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final ResourceLocation modelId;
    private final Map<PipeModelKey, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    public PipeBakedModel(ResourceLocation modelId) {
        this.modelId = modelId;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand,
        @Nonnull ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            // Nothing is culled by neighbouring blocks
            return List.of();
        }
        PipeModelKey key = data.get(TilePipeHolder.MODEL_KEY);
        if (key == null || key.definition == null) {
            return List.of();
        }
        return cache.computeIfAbsent(key, this::generate);
    }

    private List<BakedQuad> generate(PipeModelKey key) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction face : Direction.values()) {
            float size = key.connected[face.ordinal()];
            if (size > 0) {
                TextureAtlasSprite sprite = getSprite(key.definition, key.sides[face.ordinal()]);
                addArm(quads, face, size * 16, sprite);
            } else {
                TextureAtlasSprite sprite = getSprite(key.definition, key.center);
                Vector3f[] box = faceBox(face, MIN, MAX);
                addDoubleSided(quads, box[0], box[1], face, sprite);
            }
        }
        return quads;
    }

    /** Adds the four sides of the connection going out of the given face, which is length pixels long. */
    private void addArm(List<BakedQuad> quads, Direction face, float length, TextureAtlasSprite sprite) {
        float[] from = { MIN, MIN, MIN };
        float[] to = { MAX, MAX, MAX };
        int axis = face.getAxis().ordinal();
        if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            from[axis] = MAX;
            to[axis] = MAX + length;
        } else {
            from[axis] = MIN - length;
            to[axis] = MIN;
        }
        for (Direction side : Direction.values()) {
            if (side.getAxis() == face.getAxis()) {
                continue;
            }
            Vector3f[] box = faceBox(side, from, to);
            addDoubleSided(quads, box[0], box[1], side, sprite);
        }
    }

    /** @return The (flat) box covering a single face of the given box. */
    private static Vector3f[] faceBox(Direction face, float min, float max) {
        return faceBox(face, new float[] { min, min, min }, new float[] { max, max, max });
    }

    private static Vector3f[] faceBox(Direction face, float[] from, float[] to) {
        float[] f = Arrays.copyOf(from, 3);
        float[] t = Arrays.copyOf(to, 3);
        int axis = face.getAxis().ordinal();
        if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            f[axis] = t[axis];
        } else {
            t[axis] = f[axis];
        }
        return new Vector3f[] { new Vector3f(f[0], f[1], f[2]), new Vector3f(t[0], t[1], t[2]) };
    }

    /** Adds a flat face facing outwards, and the same face seen from the inside. */
    private void addDoubleSided(List<BakedQuad> quads, Vector3f from, Vector3f to, Direction face,
        TextureAtlasSprite sprite) {
        quads.add(bake(from, to, face, sprite));
        quads.add(bake(from, to, face.getOpposite(), sprite));
    }

    private BakedQuad bake(Vector3f from, Vector3f to, Direction face, TextureAtlasSprite sprite) {
        BlockFaceUV uv = new BlockFaceUV(autoUv(face, from, to), 0);
        BlockElementFace elementFace = new BlockElementFace(null, -1, "", uv);
        return BAKERY.bakeQuad(from, to, elementFace, sprite, face, BlockModelRotation.X0_Y0, null, true, modelId);
    }

    /** The UVs that vanilla gives a model element face without explicit UVs: the face's position in the block. */
    private static float[] autoUv(Direction face, Vector3f from, Vector3f to) {
        return switch (face) {
            case DOWN -> new float[] { from.x, 16 - to.z, to.x, 16 - from.z };
            case UP -> new float[] { from.x, from.z, to.x, to.z };
            case NORTH -> new float[] { 16 - to.x, 16 - to.y, 16 - from.x, 16 - from.y };
            case SOUTH -> new float[] { from.x, 16 - to.y, to.x, 16 - from.y };
            case WEST -> new float[] { from.z, 16 - to.y, to.z, 16 - from.y };
            case EAST -> new float[] { 16 - to.z, 16 - to.y, 16 - from.z, 16 - from.y };
        };
    }

    private static TextureAtlasSprite getSprite(PipeDefinition definition, PipeFaceTex tex) {
        int index = tex.getCount() > 0 ? tex.getTexture(0) : 0;
        if (index < 0 || index >= definition.textures.length) {
            index = 0;
        }
        return getSprite(definition.textures[index]);
    }

    private static TextureAtlasSprite getSprite(String location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ResourceLocation.parse(location));
    }

    // Misc model properties

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getSprite("minecraft:block/glass");
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        PipeModelKey key = data.get(TilePipeHolder.MODEL_KEY);
        if (key == null || key.definition == null) {
            return getParticleIcon();
        }
        return getSprite(key.definition, key.center);
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Nonnull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand,
        @Nonnull ModelData data) {
        return RENDER_TYPES;
    }
}
