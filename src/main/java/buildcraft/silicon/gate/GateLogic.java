/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.BCLog;
import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.statements.StatementSlot;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventActionActivate;

import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.statement.ActionWrapper;
import buildcraft.lib.statement.ActionWrapper.ActionWrapperExternal;
import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternal;
import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternalSided;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.statement.TriggerWrapper;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperExternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternalSided;

import buildcraft.silicon.plug.PluggableGate;

/** The logic of a gate: its trigger/action pairs, how they are grouped, and which ones are on. */
public class GateLogic implements IGate, IRedstoneStatementContainer {
    protected static final IdAllocator ID_ALLOC = new IdAllocator("GateLogic");
    /** Sent when any of {@link #triggerOn}, {@link #actionOn}, or {@link #connections} change. */
    public static final int NET_ID_RESOLVE = ID_ALLOC.allocId("RESOLVE");
    /** Sent when a single statement changed. */
    public static final int NET_ID_CHANGE = ID_ALLOC.allocId("STATEMENT_CHANGE");
    /** Sent when {@link #isOn} is true. */
    public static final int NET_ID_GLOWING = ID_ALLOC.allocId("GLOWING");
    /** Sent when {@link #isOn} is false. */
    public static final int NET_ID_DARK = ID_ALLOC.allocId("DARK");

    public final PluggableGate pluggable;
    public final GateVariant variant;
    public final StatementPair[] statements;
    public final List<StatementSlot> activeActions = new ArrayList<>();
    /** Whether each pair is grouped with the next one (so that the logic goes across several pairs). */
    public final boolean[] connections;
    /** Used at the client to display if an action is activated (or would be activated if it's not null), or a trigger
     * is currently triggering. */
    public final boolean[] triggerOn, actionOn;
    /** Used on the client to determine if this gate should glow or not. */
    public boolean isOn;

    public GateLogic(PluggableGate pluggable, GateVariant variant) {
        this.pluggable = pluggable;
        this.variant = variant;
        int slots = variant.numSlots();
        statements = new StatementPair[slots];
        for (int s = 0; s < slots; s++) {
            statements[s] = new StatementPair();
        }
        connections = new boolean[slots - 1];
        triggerOn = new boolean[slots];
        actionOn = new boolean[slots];
    }

    // Saving + Loading

    public GateLogic(PluggableGate pluggable, CompoundTag nbt) {
        this(pluggable, GateVariant.readFromNbt(nbt.getCompound("variant")));
        readConfigData(nbt);
    }

    public void readConfigData(CompoundTag nbt) {
        short c = nbt.getShort("connections");
        for (int i = 0; i < connections.length; i++) {
            connections[i] = ((c >>> i) & 1) == 1;
        }
        for (int i = 0; i < statements.length; i++) {
            statements[i].trigger.readFromNbt(nbt.getCompound("trigger[" + i + "]"));
            statements[i].action.readFromNbt(nbt.getCompound("action[" + i + "]"));
        }
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("variant", variant.writeToNbt());
        short c = 0;
        for (int i = 0; i < connections.length; i++) {
            if (connections[i]) {
                c |= (short) (1 << i);
            }
        }
        nbt.putShort("connections", c);
        for (int s = 0; s < statements.length; s++) {
            if (statements[s].trigger.get() != null) {
                nbt.put("trigger[" + s + "]", statements[s].trigger.writeToNbt());
            }
            if (statements[s].action.get() != null) {
                nbt.put("action[" + s + "]", statements[s].action.writeToNbt());
            }
        }
        return nbt;
    }

    // Networking

    public GateLogic(PluggableGate pluggable, FriendlyByteBuf buffer) {
        this(pluggable, GateVariant.readFromBuffer(buffer));
        readBooleans(buffer, triggerOn);
        readBooleans(buffer, actionOn);
        readBooleans(buffer, connections);
        for (StatementPair pair : statements) {
            pair.trigger.readFromBuffer(buffer);
            pair.action.readFromBuffer(buffer);
        }
        boolean on = false;
        for (int i = 0; i < statements.length; i++) {
            on |= actionOn[i] && statements[i].action.get() != null;
        }
        isOn = on;
    }

    public void writeCreationToBuf(FriendlyByteBuf buffer) {
        variant.writeToBuffer(buffer);
        writeBooleans(buffer, triggerOn);
        writeBooleans(buffer, actionOn);
        writeBooleans(buffer, connections);
        for (StatementPair pair : statements) {
            pair.trigger.writeToBuffer(buffer);
            pair.action.writeToBuffer(buffer);
        }
    }

    private static void writeBooleans(FriendlyByteBuf buffer, boolean[] values) {
        for (boolean value : values) {
            buffer.writeBoolean(value);
        }
    }

    private static void readBooleans(FriendlyByteBuf buffer, boolean[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.readBoolean();
        }
    }

    /** Reads a message sent by the server. */
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        int id = buffer.readUnsignedByte();
        if (side != LogicalSide.CLIENT) {
            BCLog.logger.warn("[silicon.gate] Unexpected message {} on the server", ID_ALLOC.getNameFor(id));
            buffer.skipBytes(buffer.readableBytes());
            return;
        }
        if (id == NET_ID_CHANGE) {
            boolean isAction = buffer.readBoolean();
            int slot = buffer.readUnsignedByte();
            StatementPair s = statements[slot];
            (isAction ? s.action : s.trigger).readFromBuffer(buffer);
        } else if (id == NET_ID_RESOLVE) {
            readBooleans(buffer, triggerOn);
            readBooleans(buffer, actionOn);
            readBooleans(buffer, connections);
        } else if (id == NET_ID_GLOWING || id == NET_ID_DARK) {
            isOn = id == NET_ID_GLOWING;
            getPipeHolder().scheduleRenderUpdate();
        } else {
            BCLog.logger.warn("[silicon.gate] Unknown ID " + ID_ALLOC.getNameFor(id));
        }
    }

    /** Sends a statement to the players that have the GUI of the gate open. */
    public void sendStatementUpdate(boolean isAction, int slot) {
        pluggable.sendGuiMessage(buffer -> {
            buffer.writeByte(NET_ID_CHANGE);
            buffer.writeBoolean(isAction);
            buffer.writeByte(slot);
            StatementPair s = statements[slot];
            (isAction ? s.action : s.trigger).writeToBuffer(buffer);
        });
    }

    public void sendResolveData() {
        pluggable.sendGuiMessage(buffer -> {
            buffer.writeByte(NET_ID_RESOLVE);
            writeBooleans(buffer, triggerOn);
            writeBooleans(buffer, actionOn);
            writeBooleans(buffer, connections);
        });
    }

    public void sendIsOn() {
        pluggable.sendMessage(buffer -> buffer.writeByte(isOn ? NET_ID_GLOWING : NET_ID_DARK));
    }

    // IGate

    @Override
    public Direction getSide() {
        return pluggable.side;
    }

    @Override
    public BlockEntity getTile() {
        return getPipeHolder().getPipeTile();
    }

    @Override
    public BlockEntity getNeighbourTile(Direction side) {
        return getPipeHolder().getNeighbourTile(side);
    }

    @Override
    public IPipeHolder getPipeHolder() {
        return pluggable.holder;
    }

    @Override
    public List<IStatement> getTriggers() {
        List<IStatement> list = new ArrayList<>(statements.length);
        for (StatementPair pair : statements) {
            TriggerWrapper e = pair.trigger.get();
            list.add(e == null ? null : e.delegate);
        }
        return list;
    }

    @Override
    public List<IStatement> getActions() {
        List<IStatement> list = new ArrayList<>(statements.length);
        for (StatementPair pair : statements) {
            ActionWrapper e = pair.action.get();
            list.add(e == null ? null : e.delegate);
        }
        return list;
    }

    @Override
    public List<StatementSlot> getActiveActions() {
        return activeActions;
    }

    @Override
    public List<IStatementParameter> getTriggerParameters(int slot) {
        return Arrays.asList(statements[slot].trigger.getParameters());
    }

    @Override
    public List<IStatementParameter> getActionParameters(int slot) {
        return Arrays.asList(statements[slot].action.getParameters());
    }

    @Override
    public int getRedstoneInput(@Nullable Direction side) {
        return getPipeHolder().getRedstoneInput(side);
    }

    @Override
    public boolean setRedstoneOutput(@Nullable Direction side, int value) {
        return getPipeHolder().setRedstoneOutput(side, value);
    }

    // Internal Logic

    /** @return True if the gate GUI should be split into 2 separate columns. */
    public boolean isSplitInTwo() {
        return variant.numSlots() > 4;
    }

    /** Checks every trigger, and activates the actions of the groups that are on. */
    public void resolveActions() {
        int groupCount = 0;
        int groupActive = 0;
        boolean prevIsOn = isOn;
        isOn = false;
        boolean[] prevTriggers = Arrays.copyOf(triggerOn, triggerOn.length);
        boolean[] prevActions = Arrays.copyOf(actionOn, actionOn.length);
        Arrays.fill(triggerOn, false);
        Arrays.fill(actionOn, false);
        activeActions.clear();

        for (int triggerIndex = 0; triggerIndex < statements.length; triggerIndex++) {
            StatementPair pair = statements[triggerIndex];
            TriggerWrapper trigger = pair.trigger.get();
            groupCount++;
            if (trigger != null && trigger.isTriggerActive(this, pair.trigger.getParameters().clone())) {
                groupActive++;
                triggerOn[triggerIndex] = true;
            }
            if (connections.length == triggerIndex || !connections[triggerIndex]) {
                boolean allActionsActive;
                if (variant.logic() == EnumGateLogic.AND) {
                    allActionsActive = groupActive == groupCount;
                } else {
                    allActionsActive = groupActive > 0;
                }
                for (int i = groupCount - 1; i >= 0; i--) {
                    int actionIndex = triggerIndex - i;
                    StatementPair fullAction = statements[actionIndex];
                    ActionWrapper action = fullAction.action.get();
                    actionOn[actionIndex] = allActionsActive;
                    if (action != null) {
                        if (allActionsActive) {
                            isOn = true;
                            StatementSlot slot = new StatementSlot();
                            slot.statement = action.delegate;
                            slot.parameters = fullAction.action.getParameters().clone();
                            slot.part = action.sourcePart;
                            activeActions.add(slot);
                            action.actionActivate(this, slot.parameters);
                            getPipeHolder().fireEvent(new PipeEventActionActivate(getPipeHolder(),
                                action.getDelegate(), slot.parameters, action.sourcePart));
                        } else {
                            action.actionDeactivated(this, fullAction.action.getParameters());
                        }
                    }
                }
                groupActive = 0;
                groupCount = 0;
            }
        }

        if (isOn != prevIsOn) {
            sendIsOn();
        }
        if (!Arrays.equals(prevTriggers, triggerOn) || !Arrays.equals(prevActions, actionOn)) {
            sendResolveData();
        }
    }

    public void onTick() {
        if (getPipeHolder().getPipeWorld().isClientSide) {
            return;
        }
        resolveActions();
    }

    public SortedSet<TriggerWrapper> getAllValidTriggers() {
        SortedSet<TriggerWrapper> set = new TreeSet<>();
        for (ITriggerInternal trigger : StatementManager.getInternalTriggers(this)) {
            if (isValidTrigger(trigger)) {
                set.add(new TriggerWrapperInternal(trigger));
            }
        }
        for (Direction face : Direction.values()) {
            for (ITriggerInternalSided trigger : StatementManager.getInternalSidedTriggers(this, face)) {
                if (isValidTrigger(trigger)) {
                    set.add(new TriggerWrapperInternalSided(trigger, face));
                }
            }
            BlockEntity neighbour = getNeighbourTile(face);
            if (neighbour != null) {
                for (ITriggerExternal trigger : StatementManager.getExternalTriggers(face, neighbour)) {
                    if (isValidTrigger(trigger)) {
                        set.add(new TriggerWrapperExternal(trigger, face));
                    }
                }
            }
        }
        return set;
    }

    public SortedSet<ActionWrapper> getAllValidActions() {
        SortedSet<ActionWrapper> set = new TreeSet<>();
        for (IActionInternal action : StatementManager.getInternalActions(this)) {
            if (isValidAction(action)) {
                set.add(new ActionWrapperInternal(action));
            }
        }
        for (Direction face : Direction.values()) {
            for (IActionInternalSided action : StatementManager.getInternalSidedActions(this, face)) {
                if (isValidAction(action)) {
                    set.add(new ActionWrapperInternalSided(action, face));
                }
            }
            BlockEntity neighbour = getNeighbourTile(face);
            if (neighbour != null) {
                for (IActionExternal action : StatementManager.getExternalActions(face, neighbour)) {
                    if (isValidAction(action)) {
                        set.add(new ActionWrapperExternal(action, face));
                    }
                }
            }
        }
        return set;
    }

    public boolean isValidTrigger(IStatement statement) {
        return statement != null && statement.minParameters() <= variant.numTriggerArgs();
    }

    public boolean isValidAction(IStatement statement) {
        return statement != null && statement.minParameters() <= variant.numActionArgs();
    }

    /** Changes a trigger (if it is valid for this gate), and tells the players that have the GUI open. Server side
     * only. */
    public void setTrigger(int slot, @Nullable TriggerWrapper trigger, IStatementParameter[] params) {
        if (slot < 0 || slot >= statements.length) {
            return;
        }
        if (trigger != null && !getAllValidTriggers().contains(trigger)) {
            return;
        }
        statements[slot].trigger.set(trigger, params);
        sendStatementUpdate(false, slot);
    }

    /** Changes an action (if it is valid for this gate), and tells the players that have the GUI open. Server side
     * only. */
    public void setAction(int slot, @Nullable ActionWrapper action, IStatementParameter[] params) {
        if (slot < 0 || slot >= statements.length) {
            return;
        }
        if (action != null && !getAllValidActions().contains(action)) {
            return;
        }
        ActionWrapper old = statements[slot].action.get();
        if (old != null && actionOn[slot]) {
            old.actionDeactivated(this, statements[slot].action.getParameters());
        }
        statements[slot].action.set(action, params);
        sendStatementUpdate(true, slot);
    }

    /** Groups (or ungroups) a pair with the next one. Server side only. */
    public void setConnected(int index, boolean connected) {
        if (index >= 0 && index < connections.length) {
            connections[index] = connected;
            sendResolveData();
        }
    }

    /** A trigger, and the action that it activates. */
    public class StatementPair {
        public final FullStatement<TriggerWrapper> trigger;
        public final FullStatement<ActionWrapper> action;

        public StatementPair() {
            trigger = new FullStatement<>(TriggerType.INSTANCE, variant.numTriggerArgs());
            action = new FullStatement<>(ActionType.INSTANCE, variant.numActionArgs());
        }
    }
}
