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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.statements.IStatementParameter;

import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperExternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternal;

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
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class GateGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos PIPE = new BlockPos(1, 1, 1);
    private static final GateVariant IRON_AND =
        new GateVariant(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];

    /** Using a gate on a pipe places it on the clicked side. It is saved, and dropped with the pipe. */
    @GameTest(template = EMPTY)
    public static void gatesArePlacedSavedAndDropped(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BCSiliconItems.gate(IRON_AND)));
        BlockPos absolute = helper.absolutePos(PIPE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.25, 0), Direction.UP, absolute,
            false);
        helper.getLevel().getBlockState(absolute).use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(pipe.getPluggable(Direction.UP) instanceof PluggableGate, "A gate should be on top");

        GateLogic gate = ((PluggableGate) pipe.getPluggable(Direction.UP)).logic;
        gate.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
        gate.setConnected(0, true);
        CompoundTag saved = pipe.saveWithoutMetadata();
        pipe.replacePluggable(Direction.UP, null);
        pipe.load(saved);
        helper.assertTrue(pipe.getPluggable(Direction.UP) instanceof PluggableGate, "The gate should be saved");
        GateLogic loaded = ((PluggableGate) pipe.getPluggable(Direction.UP)).logic;
        helper.assertTrue(loaded.variant.equals(IRON_AND), "The variant should be saved, found " + loaded.variant);
        helper.assertTrue(loaded.statements[0].trigger.get() != null
            && loaded.statements[0].trigger.get().delegate == BCCoreStatements.TRIGGER_TRUE, "The trigger too");
        helper.assertTrue(loaded.connections[0], "And the connections");

        NonNullList<ItemStack> drops = NonNullList.create();
        pipe.addDrops(drops);
        helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(BCSiliconItems.gate(IRON_AND))),
            "The gate should drop with the pipe, dropped " + drops);
        helper.succeed();
    }

    /** "Always on" with "redstone signal" makes the pipe emit redstone, which lights a lamp next to it. */
    @GameTest(template = EMPTY)
    public static void gatesEmitRedstone(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        helper.setBlock(PIPE.east(), Blocks.REDSTONE_LAMP);
        GateLogic gate = placeGate(pipe, Direction.UP, IRON_AND);
        gate.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
        gate.setAction(0, new ActionWrapperInternal(BCCoreStatements.ACTION_REDSTONE), NO_PARAMS);
        helper.succeedWhen(() -> {
            helper.assertTrue(gate.isOn, "The gate should be on");
            helper.assertBlockProperty(PIPE.east(), RedstoneLampBlock.LIT, true);
        });
    }

    /** "Redstone signal on" follows the signal that the pipe receives. */
    @GameTest(template = EMPTY)
    public static void gatesSeeRedstone(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        GateLogic gate = placeGate(pipe, Direction.UP, IRON_AND);
        gate.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_REDSTONE_ACTIVE), NO_PARAMS);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!gate.triggerOn[0], "There is no signal yet");
            helper.setBlock(PIPE.west(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(gate.triggerOn[0], "The trigger should see the redstone block");
            helper.succeed();
        });
    }

    /** "Items in inventory" looks at the chest next to the pipe. */
    @GameTest(template = EMPTY)
    public static void gatesSeeInventories(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        helper.setBlock(PIPE.east(), Blocks.REDSTONE_LAMP);
        GateLogic gate = placeGate(pipe, Direction.UP, IRON_AND);
        gate.setTrigger(0, new TriggerWrapperExternal(BCCoreStatements.TRIGGER_INVENTORY_CONTAINS, Direction.WEST),
            NO_PARAMS);
        gate.setAction(0, new ActionWrapperInternal(BCCoreStatements.ACTION_REDSTONE), NO_PARAMS);
        helper.assertTrue(gate.statements[0].trigger.get() != null, "The chest trigger should be accepted");
        helper.runAfterDelay(3, () -> {
            helper.assertBlockProperty(PIPE.east(), RedstoneLampBlock.LIT, false);
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(PIPE.west());
            chest.setItem(0, new ItemStack(Items.COBBLESTONE));
        });
        helper.runAfterDelay(6, () -> {
            helper.assertBlockProperty(PIPE.east(), RedstoneLampBlock.LIT, true);
            helper.succeed();
        });
    }

    /** Grouped pairs need every trigger with AND, and any of them with OR. */
    @GameTest(template = EMPTY)
    public static void gateLogicGroupsPairs(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        GateLogic and = placeGate(pipe, Direction.UP, IRON_AND);
        GateLogic or = placeGate(pipe, Direction.DOWN,
            new GateVariant(EnumGateLogic.OR, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER));
        for (GateLogic gate : new GateLogic[] { and, or }) {
            gate.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
            gate.setTrigger(1, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_REDSTONE_ACTIVE), NO_PARAMS);
            gate.setAction(0, new ActionWrapperInternal(BCCoreStatements.ACTION_REDSTONE), NO_PARAMS);
            gate.setConnected(0, true);
        }
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!and.isOn, "An AND gate needs both triggers");
            helper.assertTrue(or.isOn, "An OR gate only needs one");
            helper.succeed();
        });
    }

    /** Statements that the gate can't use (like a chest trigger without a chest) are refused. */
    @GameTest(template = EMPTY)
    public static void gatesRefuseInvalidStatements(GameTestHelper helper) {
        TilePipeHolder pipe = placePipe(helper);
        GateLogic gate = placeGate(pipe, Direction.UP, IRON_AND);
        gate.setTrigger(0, new TriggerWrapperExternal(BCCoreStatements.TRIGGER_INVENTORY_CONTAINS, Direction.WEST),
            NO_PARAMS);
        helper.assertTrue(gate.statements[0].trigger.get() == null, "There is no chest to look at");
        gate.setTrigger(5, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
        helper.succeed();
    }

    private static TilePipeHolder placePipe(GameTestHelper helper) {
        helper.setBlock(PIPE, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(PIPE);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get()));
        return tile;
    }

    private static GateLogic placeGate(TilePipeHolder pipe, Direction side, GateVariant variant) {
        PluggableGate gate = new PluggableGate(BCSiliconPlugs.GATE, pipe, side, variant);
        pipe.replacePluggable(side, gate);
        return gate.logic;
    }
}
