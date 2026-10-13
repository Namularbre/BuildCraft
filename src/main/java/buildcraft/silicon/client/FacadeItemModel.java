/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.BakedModelWrapper;

import buildcraft.BuildCraft;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.PluggableFacade.KeyPlugFacade;
import buildcraft.transport.client.model.PipeBakedModel;
import buildcraft.transport.client.model.PlugModels;

/** The model of the facade item: the facade (as it would be on the west side of a pipe), in the middle of the item.
 * The transforms come from the item's model file. */
public class FacadeItemModel extends BakedModelWrapper<BakedModel> {
    private static final ResourceLocation MODEL_ID = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID,
        "item/plug_facade");
    private final Map<KeyPlugFacade, BakedModel> cache = new ConcurrentHashMap<>();
    private final ItemOverrides overrides = new ItemOverrides() {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
            @Nullable LivingEntity entity, int seed) {
            FacadeInstance states = ItemPluggableFacade.getStates(stack);
            BlockState state = states.phasedStates[0].stateInfo.state;
            if (state.isAir()) {
                return FacadeItemModel.this.originalModel;
            }
            KeyPlugFacade key = new KeyPlugFacade(Direction.WEST, state, states.isHollow, 0);
            return cache.computeIfAbsent(key, FacadeItemModel.this::bake);
        }
    };

    public FacadeItemModel(BakedModel original) {
        super(original);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private BakedModel bake(KeyPlugFacade key) {
        List<BakedQuad> quads = new ArrayList<>();
        List<PlugModels.Box> boxes = PlugModels.get(key);
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        if (boxes != null) {
            for (PlugModels.Box box : boxes) {
                // Moved from the west side to the middle
                Vector3f from = new Vector3f(box.from()[0] + 7, box.from()[1], box.from()[2]);
                Vector3f to = new Vector3f(box.to()[0] + 7, box.to()[1], box.to()[2]);
                for (Direction face : Direction.values()) {
                    Vector3f[] flat = flat(face, from, to);
                    for (PlugModels.Layer layer : box.getLayers(face)) {
                        TextureAtlasSprite sprite = atlas.apply(layer.texture());
                        quads.add(PipeBakedModel.bakeFace(flat[0], flat[1], face, sprite, layer.tintIndex(),
                            MODEL_ID));
                    }
                }
            }
        }
        return new BakedModelWrapper<>(originalModel) {
            @Nonnull
            @Override
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                @Nonnull RandomSource rand) {
                return side == null ? quads : List.of();
            }

            @Override
            public boolean useAmbientOcclusion() {
                return false;
            }
        };
    }

    private static Vector3f[] flat(Direction face, Vector3f from, Vector3f to) {
        Vector3f f = new Vector3f(from);
        Vector3f t = new Vector3f(to);
        switch (face) {
            case DOWN -> t.y = f.y;
            case UP -> f.y = t.y;
            case NORTH -> t.z = f.z;
            case SOUTH -> f.z = t.z;
            case WEST -> t.x = f.x;
            case EAST -> f.x = t.x;
        }
        return new Vector3f[] { f, t };
    }
}
