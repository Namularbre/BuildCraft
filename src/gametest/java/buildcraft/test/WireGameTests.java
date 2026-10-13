/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.EnumWirePart;

import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternal;

import buildcraft.BuildCraft;
import buildcraft.core.statements.BCCoreStatements;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.statements.ActionParameterSignal;
import buildcraft.transport.statements.ActionPipeSignal;
import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.EnumWireBetween;

/** Pipe wires: 3 pipes in a row (west to east), with a red wire along their upper south edge. A gate on the west pipe
 * powers it, and a gate on the east pipe sees it. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class WireGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos WEST = new BlockPos(0, 1, 1), MIDDLE = new BlockPos(1, 1, 1);
    private static final BlockPos EAST = new BlockPos(2, 1, 1);
    private static final GateVariant IRON_AND =
        new GateVariant(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];
    private static final int RED_ON = DyeColor.RED.ordinal() * 2;
    private static final int RED_OFF = DyeColor.RED.ordinal() * 2 + 1;
    private static final int BLUE_ON = DyeColor.BLUE.ordinal() * 2;
    private static final ActionPipeSignal RED_SIGNAL =
        BCTransportStatements.ACTION_PIPE_SIGNAL[DyeColor.RED.ordinal()];

    /** The signal goes from one gate to the other, and stops when the wire is cut. */
    @GameTest(template = EMPTY)
    public static void signalsGoAlongWires(GameTestHelper helper) {
        TilePipeHolder[] pipes = placeWiredPipes(helper);
        TilePipeHolder middle = pipes[1];
        helper.assertTrue(pipes[0].wireManager.betweens.get(EnumWireBetween.EAST_UP_SOUTH) == DyeColor.RED,
            "The wire should go into the next pipe");

        GateLogic emitter = placeGate(pipes[0]);
        emitter.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
        // Also powers blue, but there aren't any blue wires
        emitter.setAction(0, new ActionWrapperInternal(RED_SIGNAL),
            new IStatementParameter[] { ActionParameterSignal.get(DyeColor.BLUE) });
        helper.assertTrue(emitter.statements[0].action.get() != null, "Gates on wired pipes can emit signals");

        GateLogic receiver = placeGate(pipes[2]);
        receiver.setTrigger(0, new TriggerWrapperInternal(BCTransportStatements.TRIGGER_PIPE_SIGNAL[RED_ON]),
            NO_PARAMS);
        receiver.setTrigger(1, new TriggerWrapperInternal(BCTransportStatements.TRIGGER_PIPE_SIGNAL[BLUE_ON]),
            NO_PARAMS);
        helper.assertTrue(receiver.statements[0].trigger.get() != null, "Wired pipes have signal triggers");
        helper.assertTrue(receiver.statements[1].trigger.get() == null, "But only for the colours of their wires");
        receiver.setTrigger(1, new TriggerWrapperInternal(BCTransportStatements.TRIGGER_PIPE_SIGNAL[RED_OFF]),
            NO_PARAMS);

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(middle.wireManager.isAnyPowered(DyeColor.RED), "The wire should be powered");
            helper.assertTrue(receiver.triggerOn[0] && !receiver.triggerOn[1], "The other gate should see it");

            NonNullList<ItemStack> drops = NonNullList.create();
            middle.wireManager.removePart(EnumWirePart.WEST_UP_SOUTH);
            middle.addDrops(drops);
            helper.assertTrue(drops.stream().anyMatch(s -> s.getItem() == BCTransportItems.wire(DyeColor.RED)),
                "Pipes drop their wires");
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!pipes[2].wireManager.isAnyPowered(DyeColor.RED), "The wire was cut");
            helper.assertTrue(!receiver.triggerOn[0] && receiver.triggerOn[1], "The other gate should see it");
            helper.succeed();
        });
    }

    /** Wires only carry a signal while a gate emits it. */
    @GameTest(template = EMPTY)
    public static void wiresAreOffWithoutEmitters(GameTestHelper helper) {
        TilePipeHolder[] pipes = placeWiredPipes(helper);
        GateLogic emitter = placeGate(pipes[0]);
        emitter.setAction(0, new ActionWrapperInternal(RED_SIGNAL),
            NO_PARAMS);
        helper.runAfterDelay(3, () -> {
            for (TilePipeHolder pipe : pipes) {
                helper.assertTrue(!pipe.wireManager.isAnyPowered(DyeColor.RED), "No trigger, so no signal");
            }
            helper.succeed();
        });
    }

    /** Places the pipes, with the wire through them. */
    private static TilePipeHolder[] placeWiredPipes(GameTestHelper helper) {
        TilePipeHolder west = placePipe(helper, WEST);
        TilePipeHolder middle = placePipe(helper, MIDDLE);
        TilePipeHolder east = placePipe(helper, EAST);
        west.wireManager.addPart(EnumWirePart.EAST_UP_SOUTH, DyeColor.RED);
        middle.wireManager.addPart(EnumWirePart.WEST_UP_SOUTH, DyeColor.RED);
        middle.wireManager.addPart(EnumWirePart.EAST_UP_SOUTH, DyeColor.RED);
        east.wireManager.addPart(EnumWirePart.WEST_UP_SOUTH, DyeColor.RED);
        // The pipes have to connect first
        for (TilePipeHolder pipe : new TilePipeHolder[] { west, middle, east }) {
            pipe.tick();
        }
        for (TilePipeHolder pipe : new TilePipeHolder[] { west, middle, east }) {
            pipe.tick();
        }
        return new TilePipeHolder[] { west, middle, east };
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos) {
        Item pipeItem = BCTransportItems.PIPE_STONE_ITEM.get();
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }

    private static GateLogic placeGate(TilePipeHolder pipe) {
        PluggableGate gate = new PluggableGate(BCSiliconPlugs.GATE, pipe, Direction.NORTH, IRON_AND);
        pipe.replacePluggable(Direction.NORTH, gate);
        return gate.logic;
    }
}
