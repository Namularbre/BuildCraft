/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.item.ItemPluggableSimple;
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
    public static final RegistryObject<ItemPipeHolder> PIPE_DIAMOND_ITEM = pipe(BCTransportPipes.DIAMOND_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_VOID_ITEM = pipe(BCTransportPipes.VOID_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_LAPIS_ITEM = pipe(BCTransportPipes.LAPIS_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_DAIZULI_ITEM = pipe(BCTransportPipes.DAIZULI_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_CLAY_ITEM = pipe(BCTransportPipes.CLAY_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_STRIPES_ITEM = pipe(BCTransportPipes.STRIPES_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_OBSIDIAN_ITEM = pipe(BCTransportPipes.OBSIDIAN_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_DIAMOND_WOOD_ITEM =
        pipe(BCTransportPipes.DIAMOND_WOOD_ITEM);
    public static final RegistryObject<ItemPipeHolder> PIPE_EMZULI_ITEM = pipe(BCTransportPipes.EMZULI_ITEM);

    public static final RegistryObject<ItemPipeHolder> PIPE_WOOD_POWER = pipe(BCTransportPipes.WOOD_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_COBBLESTONE_POWER =
        pipe(BCTransportPipes.COBBLESTONE_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_STONE_POWER = pipe(BCTransportPipes.STONE_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_QUARTZ_POWER = pipe(BCTransportPipes.QUARTZ_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_GOLD_POWER = pipe(BCTransportPipes.GOLD_POWER);
    public static final RegistryObject<ItemPipeHolder> PIPE_SANDSTONE_POWER = pipe(BCTransportPipes.SANDSTONE_POWER);

    public static final RegistryObject<ItemPipeHolder> PIPE_WOOD_FLUID = pipe(BCTransportPipes.WOOD_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_COBBLESTONE_FLUID =
        pipe(BCTransportPipes.COBBLESTONE_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_STONE_FLUID = pipe(BCTransportPipes.STONE_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_QUARTZ_FLUID = pipe(BCTransportPipes.QUARTZ_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_GOLD_FLUID = pipe(BCTransportPipes.GOLD_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_SANDSTONE_FLUID = pipe(BCTransportPipes.SANDSTONE_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_IRON_FLUID = pipe(BCTransportPipes.IRON_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_CLAY_FLUID = pipe(BCTransportPipes.CLAY_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_DIAMOND_WOOD_FLUID =
        pipe(BCTransportPipes.DIAMOND_WOOD_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_DIAMOND_FLUID = pipe(BCTransportPipes.DIAMOND_FLUID);
    public static final RegistryObject<ItemPipeHolder> PIPE_VOID_FLUID = pipe(BCTransportPipes.VOID_FLUID);

    public static final RegistryObject<ItemPipeHolder> PIPE_STRUCTURE = pipe(BCTransportPipes.STRUCTURE);

    public static final RegistryObject<ItemPluggableSimple> PLUG_BLOCKER = BCRegistries.ITEMS.register(
        "plug_blocker", () -> new ItemPluggableSimple(new Item.Properties(), BCTransportPlugs.BLOCKER)
    );
    public static final RegistryObject<ItemPluggableSimple> PLUG_POWER_ADAPTOR = BCRegistries.ITEMS.register(
        "plug_power_adaptor", () -> new ItemPluggableSimple(new Item.Properties(), BCTransportPlugs.POWER_ADAPTOR)
    );

    /** Turns item pipes into fluid pipes. */
    public static final RegistryObject<Item> PIPE_SEALANT =
        BCRegistries.ITEMS.register("pipe_sealant", () -> new Item(new Item.Properties()));

    /** The pipe wires, by colour. */
    public static final Map<DyeColor, RegistryObject<Item>> WIRES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor colour : DyeColor.values()) {
            WIRES.put(colour, BCRegistries.ITEMS.register(
                "wire_" + colour.getName(), () -> new Item(new Item.Properties())
            ));
        }
    }

    private BCTransportItems() {}

    public static Item wire(DyeColor colour) {
        return WIRES.get(colour).get();
    }

    /** @return The colour of the given wire item, or null if it isn't one. */
    @Nullable
    public static DyeColor getWireColour(Item item) {
        for (Map.Entry<DyeColor, RegistryObject<Item>> entry : WIRES.entrySet()) {
            if (entry.getValue().get() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

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
