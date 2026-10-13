/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import java.util.Arrays;

import net.minecraft.core.Direction;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeApi.PowerTransferInfo;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeDefinition.IPipeCreator;
import buildcraft.api.transport.pipe.PipeDefinition.IPipeLoader;
import buildcraft.api.transport.pipe.PipeDefinition.PipeDefinitionBuilder;
import buildcraft.api.transport.pipe.PipeFlowType;

import buildcraft.BuildCraft;
import buildcraft.transport.pipe.PipeRegistry;
import buildcraft.transport.pipe.behaviour.PipeBehaviourCobble;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDaizuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamondFluid;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamondItem;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourGold;
import buildcraft.transport.pipe.behaviour.PipeBehaviourIron;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLapis;
import buildcraft.transport.pipe.behaviour.PipeBehaviourQuartz;
import buildcraft.transport.pipe.behaviour.PipeBehaviourSandstone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStone;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStructure;
import buildcraft.transport.pipe.behaviour.PipeBehaviourVoid;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodPower;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.PipeFlowStructure;

/** Every pipe definition. Must be loaded while BuildCraft is the active mod, as definitions take their namespace from
 * it. */
public final class BCTransportPipes {
    /** The flow rate of the slowest fluid pipe, in millibuckets per tick. */
    public static final int BASE_FLUID_FLOW_RATE = 10;

    static {
        PipeApi.pipeRegistry = PipeRegistry.INSTANCE;
        PipeApi.flowItems = new PipeFlowType(PipeFlowItems::new, PipeFlowItems::new);
        PipeApi.flowPower = new PipeFlowType(PipeFlowPower::new, PipeFlowPower::new);
        PipeApi.flowFluids = new PipeFlowType(PipeFlowFluids::new, PipeFlowFluids::new);
        PipeApi.flowStructure = new PipeFlowType(PipeFlowStructure::new, PipeFlowStructure::new);
    }

    public static final PipeDefinition STRUCTURE = new PipeDefinitionBuilder(BuildCraft.MODID)
        .logic(PipeBehaviourStructure::new, PipeBehaviourStructure::new).enableColouring()
        .id("structure").tex("structure").flow(PipeApi.flowStructure).define();

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
    public static final PipeDefinition DIAMOND_ITEM = diamondPipe("diamond_item", false);
    public static final PipeDefinition VOID_ITEM = itemPipe(
        "void_item", PipeBehaviourVoid::new, PipeBehaviourVoid::new
    );
    /** One texture for each colour (in {@link net.minecraft.world.item.DyeColor#getId()} order). */
    private static final String[] COLOUR_SUFFIXES = {
        "_white", "_orange", "_magenta", "_light_blue", "_yellow", "_lime", "_pink", "_gray",
        "_light_gray", "_cyan", "_purple", "_blue", "_brown", "_green", "_red", "_black"
    };
    public static final PipeDefinition LAPIS_ITEM = itemPipe(
        "lapis_item", PipeBehaviourLapis::new, PipeBehaviourLapis::new, COLOUR_SUFFIXES
    );
    public static final PipeDefinition DIAMOND_WOOD_ITEM = itemPipe(
        "diamond_wood_item", PipeBehaviourWoodDiamond::new, PipeBehaviourWoodDiamond::new, "_clear", "_filled"
    );
    public static final PipeDefinition EMZULI_ITEM = itemPipe(
        "emzuli_item", PipeBehaviourEmzuli::new, PipeBehaviourEmzuli::new, "_clear", "_filled"
    );
    public static final PipeDefinition DAIZULI_ITEM = itemPipe(
        "daizuli_item", PipeBehaviourDaizuli::new, PipeBehaviourDaizuli::new, withFilled(COLOUR_SUFFIXES)
    );

    public static final PipeDefinition WOOD_POWER = powerPipe(
        "wood_power", PipeBehaviourWoodPower::new, PipeBehaviourWoodPower::new, 16, 128, true, "_clear", "_filled"
    );
    public static final PipeDefinition COBBLESTONE_POWER = powerPipe(
        "cobblestone_power", PipeBehaviourCobble::new, PipeBehaviourCobble::new, 4, 16, false
    );
    public static final PipeDefinition STONE_POWER = powerPipe(
        "stone_power", PipeBehaviourStone::new, PipeBehaviourStone::new, 8, 32, false
    );
    public static final PipeDefinition QUARTZ_POWER = powerPipe(
        "quartz_power", PipeBehaviourQuartz::new, PipeBehaviourQuartz::new, 32, 32, false
    );
    public static final PipeDefinition GOLD_POWER = powerPipe(
        "gold_power", PipeBehaviourGold::new, PipeBehaviourGold::new, 64, 32, false
    );
    public static final PipeDefinition SANDSTONE_POWER = powerPipe(
        "sandstone_power", PipeBehaviourSandstone::new, PipeBehaviourSandstone::new, 16, 32, false
    );

    public static final PipeDefinition WOOD_FLUID = fluidPipe(
        "wood_fluid", PipeBehaviourWood::new, PipeBehaviourWood::new, 1, 10, "_clear", "_filled"
    );
    public static final PipeDefinition COBBLESTONE_FLUID = fluidPipe(
        "cobblestone_fluid", PipeBehaviourCobble::new, PipeBehaviourCobble::new, 2, 10
    );
    public static final PipeDefinition STONE_FLUID = fluidPipe(
        "stone_fluid", PipeBehaviourStone::new, PipeBehaviourStone::new, 2, 10
    );
    public static final PipeDefinition QUARTZ_FLUID = fluidPipe(
        "quartz_fluid", PipeBehaviourQuartz::new, PipeBehaviourQuartz::new, 4, 10
    );
    public static final PipeDefinition GOLD_FLUID = fluidPipe(
        "gold_fluid", PipeBehaviourGold::new, PipeBehaviourGold::new, 8, 2
    );
    public static final PipeDefinition SANDSTONE_FLUID = fluidPipe(
        "sandstone_fluid", PipeBehaviourSandstone::new, PipeBehaviourSandstone::new, 2, 10
    );
    public static final PipeDefinition DIAMOND_WOOD_FLUID = fluidPipe(
        "diamond_wood_fluid", PipeBehaviourWoodDiamond::new, PipeBehaviourWoodDiamond::new, 1, 10, "_clear", "_filled"
    );
    public static final PipeDefinition IRON_FLUID = fluidPipe(
        "iron_fluid", PipeBehaviourIron::new, PipeBehaviourIron::new, 4, 10, "_clear", "_filled"
    );
    public static final PipeDefinition DIAMOND_FLUID = diamondPipe("diamond_fluid", true);
    public static final PipeDefinition VOID_FLUID = fluidPipe(
        "void_fluid", PipeBehaviourVoid::new, PipeBehaviourVoid::new, 8, 10
    );

    private BCTransportPipes() {}

    private static String[] withFilled(String[] suffixes) {
        String[] all = Arrays.copyOf(suffixes, suffixes.length + 1);
        all[suffixes.length] = "_filled";
        return all;
    }

    private static PipeDefinition itemPipe(String id, IPipeCreator creator, IPipeLoader loader,
        String... textureSuffixes) {
        PipeDefinitionBuilder builder = new PipeDefinitionBuilder(BuildCraft.MODID);
        builder.logic(creator, loader);
        builder.enableColouring();
        return builder.id(id).tex(id, textureSuffixes).flowItem().define();
    }

    /** @param flowMultiplier The pipe moves this many times the base flow rate (10 mB) per tick.
     * @param delay How long fluid takes to cross each section of the pipe, in ticks. */
    private static PipeDefinition fluidPipe(String id, IPipeCreator creator, IPipeLoader loader, int flowMultiplier,
        int delay, String... textureSuffixes) {
        PipeDefinitionBuilder builder = new PipeDefinitionBuilder(BuildCraft.MODID);
        builder.logic(creator, loader);
        builder.enableColouring();
        PipeDefinition definition = builder.id(id).tex(id, textureSuffixes).flowFluid().define();
        PipeApi.fluidTransferData.put(
            definition, new PipeApi.FluidTransferInfo(BASE_FLUID_FLOW_RATE * flowMultiplier, delay)
        );
        return definition;
    }

    /** @param mjPerTick The maximum power the pipe can move each tick, in MJ.
     * @param resistanceDivisor The pipe's resistance is 1 MJ divided by this, per MJ moved.
     * @param receiver True if the pipe accepts power from engines. */
    private static PipeDefinition powerPipe(String id, IPipeCreator creator, IPipeLoader loader, int mjPerTick,
        int resistanceDivisor, boolean receiver, String... textureSuffixes) {
        PipeDefinitionBuilder builder = new PipeDefinitionBuilder(BuildCraft.MODID);
        builder.logic(creator, loader);
        builder.enableColouring();
        PipeDefinition definition = builder.id(id).tex(id, textureSuffixes).flowPower().define();
        PipeApi.powerTransferData.put(
            definition, PowerTransferInfo.createFromResistance(
                MjAPI.MJ * mjPerTick, MjAPI.MJ / resistanceDivisor, receiver
            )
        );
        return definition;
    }

    /** Diamond pipes have a texture for each side (the colour of its filters in the GUI), and one for the item. */
    private static PipeDefinition diamondPipe(String id, boolean fluid) {
        String[] suffixes = new String[8];
        suffixes[0] = "";
        for (Direction face : Direction.values()) {
            suffixes[face.ordinal() + 1] = "_" + face.getSerializedName();
        }
        suffixes[7] = "_itemstack";
        PipeDefinitionBuilder builder = new PipeDefinitionBuilder(BuildCraft.MODID);
        builder.enableColouring();
        if (fluid) {
            builder.logic(PipeBehaviourDiamondFluid::new, PipeBehaviourDiamondFluid::new);
            PipeDefinition definition = builder.id(id).tex(id, suffixes).itemTex(7).flowFluid().define();
            PipeApi.fluidTransferData.put(definition, new PipeApi.FluidTransferInfo(BASE_FLUID_FLOW_RATE * 2, 10));
            return definition;
        }
        builder.logic(PipeBehaviourDiamondItem::new, PipeBehaviourDiamondItem::new);
        return builder.id(id).tex(id, suffixes).itemTex(7).flowItem().define();
    }

    /** Loads this class, so that every pipe is defined. */
    static void init() {}
}
