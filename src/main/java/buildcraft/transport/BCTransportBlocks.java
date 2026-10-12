/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.tile.TilePipeHolder;

public final class BCTransportBlocks {
    public static final RegistryObject<BlockPipeHolder> PIPE_HOLDER = BCRegistries.BLOCKS.register(
        "pipe_holder", () -> new BlockPipeHolder(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.25f).sound(SoundType.GLASS)
                .pushReaction(PushReaction.BLOCK)
        )
    );

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<TilePipeHolder>> PIPE_HOLDER_TILE =
        BCRegistries.BLOCK_ENTITY_TYPES.register(
            "pipe_holder", () -> BlockEntityType.Builder.of(TilePipeHolder::new, PIPE_HOLDER.get()).build(null)
        );

    private BCTransportBlocks() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
