/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.BuildCraft;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeDefinition.IPipeCreator;
import buildcraft.api.transport.pipe.PipeDefinition.IPipeLoader;
import buildcraft.api.transport.pipe.PipeDefinition.PipeDefinitionBuilder;
import buildcraft.api.transport.pipe.PipeFlowType;

import buildcraft.transport.pipe.PipeRegistry;
import buildcraft.transport.pipe.behaviour.PipeBehaviourCobble;
import buildcraft.transport.pipe.behaviour.PipeBehaviourGold;
import buildcraft.transport.pipe.behaviour.PipeBehaviourIron;
import buildcraft.transport.pipe.behaviour.PipeBehaviourQuartz;
import buildcraft.transport.pipe.behaviour.PipeBehaviourSandstone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourVoid;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;
import buildcraft.transport.pipe.flow.PipeFlowItems;

/** Every pipe definition. Must be loaded while BuildCraft is the active mod, as definitions take their namespace from
 * it. */
public final class BCTransportPipes {
    static {
        PipeApi.pipeRegistry = PipeRegistry.INSTANCE;
        PipeApi.flowItems = new PipeFlowType(PipeFlowItems::new, PipeFlowItems::new);
    }

    public static final PipeDefinition WOOD_ITEM = itemPipe(
        "wood_item", PipeBehaviourWood::new, PipeBehaviourWood::new, "_clear", "_filled"
    );
    public static final PipeDefinition COBBLESTONE_ITEM = itemPipe(
        "cobblestone_item", PipeBehaviourCobble::new, PipeBehaviourCobble::new
    );
    public static final PipeDefinition STONE_ITEM = itemPipe(
        "stone_item", PipeBehaviourStone::new, PipeBehaviourStone::new
    );
    public static final PipeDefinition QUARTZ_ITEM = itemPipe(
        "quartz_item", PipeBehaviourQuartz::new, PipeBehaviourQuartz::new
    );
    public static final PipeDefinition GOLD_ITEM = itemPipe(
        "gold_item", PipeBehaviourGold::new, PipeBehaviourGold::new
    );
    public static final PipeDefinition SANDSTONE_ITEM = itemPipe(
        "sandstone_item", PipeBehaviourSandstone::new, PipeBehaviourSandstone::new
    );
    public static final PipeDefinition IRON_ITEM = itemPipe(
        "iron_item", PipeBehaviourIron::new, PipeBehaviourIron::new, "_clear", "_filled"
    );
    public static final PipeDefinition VOID_ITEM = itemPipe(
        "void_item", PipeBehaviourVoid::new, PipeBehaviourVoid::new
    );

    private BCTransportPipes() {}

    private static PipeDefinition itemPipe(String id, IPipeCreator creator, IPipeLoader loader,
        String... textureSuffixes) {
        PipeDefinitionBuilder builder = new PipeDefinitionBuilder(BuildCraft.MODID);
        builder.logic(creator, loader);
        builder.enableColouring();
        return builder.id(id).tex(id, textureSuffixes).flowItem().define();
    }

    /** Loads this class, so that every pipe is defined. */
    static void init() {}
}
