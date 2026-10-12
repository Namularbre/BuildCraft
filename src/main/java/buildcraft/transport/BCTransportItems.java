/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.transport.item.ItemPipeHolder;

public final class BCTransportItems {
    /** The item of every pipe, in definition order. */
    public static final Map<PipeDefinition, RegistryObject<ItemPipeHolder>> PIPES = new LinkedHashMap<>();

    public static final RegistryObject<ItemPipeHolder> PIPE_WOOD_ITEM = pipe(BCTransportPipes.WOOD_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_COBBLESTONE_ITEM = pipe(BCTransportPipes.COBBLESTONE_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_STONE_ITEM = pipe(BCTransportPipes.STONE_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_QUARTZ_ITEM = pipe(BCTransportPipes.QUARTZ_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_GOLD_ITEM = pipe(BCTransportPipes.GOLD_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_SANDSTONE_ITEM = pipe(BCTransportPipes.SANDSTONE_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_IRON_ITEM = pipe(BCTransportPipes.IRON_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_VOID_ITEM = pipe(BCTransportPipes.VOID_ITEM);

    public static final RegistryObject<ItemPipeHolder> PIPE_WOOD_POWER = pipe(BCTransportPipes.WOOD_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_COBBLESTONE_POWER =
        pipe(BCTransportPipes.COBBLESTONE_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_STONE_POWER = pipe(BCTransportPipes.STONE_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_QUARTZ_POWER = pipe(BCTransportPipes.QUARTZ_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_GOLD_POWER = pipe(BCTransportPipes.GOLD_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_SANDSTONE_POWER = pipe(BCTransportPipes.SANDSTONE_POWER);

    private BCTransportItems() {}

    /** Registers the item of a pipe, named after its definition (for example "wood_item_pipe"). */
    private static RegistryObject<ItemPipeHolder> pipe(PipeDefinition definition) {
        RegistryObject<ItemPipeHolder> item = BCRegistries.ITEMS.register(
            definition.identifier.getPath() + "_pipe", () -> {
                ItemPipeHolder pipe = new ItemPipeHolder(
                    BCTransportBlocks.PIPE_HOLDER.get(), definition, new Item.Properties()
                );
                PipeApi.pipeRegistry.setItemForPipe(definition, pipe);
                return pipe;
            }
        );
        PIPES.put(definition, item);
        return item;
    }

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
