/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternalSided;

import buildcraft.BuildCraft;
import buildcraft.core.statements.BCCoreStatements;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.plug.PluggableLightSensor;
import buildcraft.silicon.plug.PluggablePulsar;
import buildcraft.silicon.statement.BCSiliconStatements;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.tile.TilePipeHolder;

/** The statements that pipes and pluggables give to gates. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class GatePipeGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos PIPE = new BlockPos(1, 1, 1);
    private static final GateVariant IRON_AND =
        new GateVariant(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];

    /** "Items traversing" sees the items that go through the pipe. */
    @GameTest(template = EMPTY)
    public static void gatesSeeItemsInPipes(GameTestHelper helper) {
        // The item comes from the west chest (pipes only accept items from the sides they are connected to)
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        helper.setBlock(PIPE.east(), Blocks.CHEST);
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_STONE_ITEM.get());
        GateLogic gate = placeGate(pipe, Direction.UP);
        gate.setTrigger(0, new TriggerWrapperInternal(BCTransportStatements.TRIGGER_ITEMS_TRAVERSING), NO_PARAMS);
        helper.assertTrue(gate.statements[0].trigger.get() != null, "Item pipes should have the trigger");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!gate.triggerOn[0], "The pipe is empty");
            IInjectable injectable = pipe.getCapability(PipeApi.CAP_INJECTABLE, Direction.WEST)
                .orElseThrow(() -> new GameTestAssertException("The pipe should accept items"));
            injectable.injectItem(new ItemStack(Items.COBBLESTONE), true, Direction.WEST, null, 0.02);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(gate.triggerOn[0], "The trigger should see the item");
            helper.succeed();
        });
    }

    /** "Face a side" turns iron pipes. */
    @GameTest(template = EMPTY)
    public static void gatesTurnIronPipes(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_IRON_ITEM.get());
        helper.setBlock(PIPE.east(), Blocks.CHEST);
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        GateLogic gate = placeGate(pipe, Direction.UP);
        helper.runAfterDelay(3, () -> {
            gate.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
            for (Direction side : new Direction[] { Direction.EAST, Direction.WEST }) {
                // Whichever side the pipe faces now, make it face the other one
                if (!side.getName().equals(getIronDirection(pipe))) {
                    gate.setAction(0, new ActionWrapperInternal(
                        BCTransportStatements.ACTION_PIPE_DIRECTION[side.ordinal()]
                    ), NO_PARAMS);
                    helper.assertTrue(gate.statements[0].action.get() != null, "The pipe can face " + side);
                    helper.runAfterDelay(3, () -> {
                        helper.assertTrue(side.getName().equals(getIronDirection(pipe)),
                            "The pipe should face " + side + ", but faces " + getIronDirection(pipe));
                        helper.succeed();
                    });
                    return;
                }
            }
        });
    }

    private static String getIronDirection(TilePipeHolder pipe) {
        return pipe.saveWithoutMetadata().getCompound("pipe").getCompound("beh").getString("currentDir");
    }

    /** A pulsar turned on by hand powers a wooden pipe, which pulls items out of a chest. */
    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void pulsarsPowerWoodenPipes(GameTestHelper helper) {
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        ((ChestBlockEntity) helper.getBlockEntity(PIPE.west())).setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        TilePipeHolder wood = placePipe(helper, PIPE, BCTransportItems.PIPE_WOOD_ITEM.get());
        placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(PIPE.east().above(), Blocks.CHEST);

        PipePluggable pulsar = placeWithItem(helper, wood, Direction.UP, BCSiliconItems.PLUG_PULSAR.get());
        helper.assertTrue(pulsar instanceof PluggablePulsar, "Pulsars go on wooden pipes");
        pulsar.onPluggableActivate(helper.makeMockPlayer(), null, 0, 0, 0);
        helper.assertTrue(((PluggablePulsar) pulsar).isManuallyEnabled(), "Clicking turns the pulsar on");

        TilePipeHolder stone = (TilePipeHolder) helper.getBlockEntity(PIPE.east());
        helper.assertTrue(placeWithItem(helper, stone, Direction.UP, BCSiliconItems.PLUG_PULSAR.get()) == null,
            "Pulsars don't go on stone pipes");
        helper.succeedWhen(() -> {
            ChestBlockEntity target = (ChestBlockEntity) helper.getBlockEntity(PIPE.east().above());
            helper.assertTrue(!target.isEmpty(), "Items should reach the chest");
        });
    }

    /** Lenses only go on item pipes. */
    @GameTest(template = EMPTY)
    public static void lensesOnlyGoOnItemPipes(GameTestHelper helper) {
        TilePipeHolder items = placePipe(helper, PIPE, BCTransportItems.PIPE_STONE_ITEM.get());
        TilePipeHolder fluids = placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STONE_FLUID.get());
        Item lens = BCSiliconItems.lens(DyeColor.RED, false);
        helper.assertTrue(placeWithItem(helper, items, Direction.UP, lens) != null, "Lenses go on item pipes");
        helper.assertTrue(placeWithItem(helper, fluids, Direction.UP, lens) == null, "But not on fluid pipes");
        helper.succeed();
    }

    /** Light sensors give the gates of the pipe light triggers, for their side. */
    @GameTest(template = EMPTY)
    public static void lightSensorsSeeLight(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper, PIPE, BCTransportItems.PIPE_STONE_ITEM.get());
        pipe.replacePluggable(Direction.UP,
            new PluggableLightSensor(BCSiliconPlugs.LIGHT_SENSOR, pipe, Direction.UP));
        GateLogic gate = placeGate(pipe, Direction.NORTH);
        gate.setTrigger(0, new TriggerWrapperInternalSided(BCSiliconStatements.TRIGGER_LIGHT_HIGH, Direction.UP),
            NO_PARAMS);
        gate.setTrigger(1, new TriggerWrapperInternalSided(BCSiliconStatements.TRIGGER_LIGHT_HIGH, Direction.DOWN),
            NO_PARAMS);
        helper.assertTrue(gate.statements[0].trigger.get() != null, "The sensor's side should have the trigger");
        helper.assertTrue(gate.statements[1].trigger.get() == null, "Other sides shouldn't");
        helper.runAfterDelay(3, () -> {
            boolean bright = helper.getLevel().getMaxLocalRawBrightness(helper.absolutePos(PIPE.above())) >= 8;
            helper.assertTrue(gate.triggerOn[0] == bright, "The trigger should follow the light");
            helper.succeed();
        });
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }

    private static GateLogic placeGate(TilePipeHolder pipe, Direction side) {
        PluggableGate gate = new PluggableGate(BCSiliconPlugs.GATE, pipe, side, IRON_AND);
        pipe.replacePluggable(side, gate);
        return gate.logic;
    }

    /** @return The pluggable that the item placed, or null if it refused. */
    private static PipePluggable placeWithItem(GameTestHelper helper, TilePipeHolder pipe, Direction side, Item item) {
        PipePluggable plug = ((IItemPluggable) item).onPlace(new ItemStack(item), pipe, side,
            helper.makeMockPlayer(), InteractionHand.MAIN_HAND);
        if (plug != null) {
            pipe.replacePluggable(side, plug);
        }
        return plug;
    }
}
