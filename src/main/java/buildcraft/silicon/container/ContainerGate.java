/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.statement.ActionWrapper;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.statement.StatementTypeParam;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.lib.statement.TriggerWrapper;

import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.gate.ActionType;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.TriggerType;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.transport.tile.TilePipeHolder;

/** The menu of a gate: its statements are changed through messages, and the possible statements are sent once. */
public class ContainerGate extends ContainerBCTile<TilePipeHolder> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("gate");
    public static final int ID_CONNECTION = IDS.allocId("CONNECTION");
    public static final int ID_VALID_STATEMENTS = IDS.allocId("VALID_STATEMENTS");
    public static final int ID_SET_TRIGGER = IDS.allocId("SET_TRIGGER");
    public static final int ID_SET_ACTION = IDS.allocId("SET_ACTION");

    public final Direction side;
    private final GateLogic gate;
    /** The statements that can be chosen. Only filled on the client once the server has sent them. */
    public final SortedSet<TriggerWrapper> possibleTriggers = new TreeSet<>();
    public final SortedSet<ActionWrapper> possibleActions = new TreeSet<>();

    public ContainerGate(int id, Player player, TilePipeHolder tile, Direction side) {
        super(BCSiliconMenus.GATE.get(), id, player, tile);
        this.side = side;
        if (!(tile.getPluggable(side) instanceof PluggableGate plug)) {
            throw new IllegalStateException("No gate on the " + side + " side of " + tile.getBlockPos());
        }
        this.gate = plug.logic;
        addFullPlayerInventory(33 + getRowCount() * 18);
    }

    /** The client-side constructor. */
    public ContainerGate(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TilePipeHolder.class), buffer.readEnum(Direction.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    /** @return The gate that this menu shows (the current one, if the pipe was sent again since). */
    public GateLogic getGate() {
        if (tile.getPluggable(side) instanceof PluggableGate plug && plug.logic.variant.equals(gate.variant)) {
            return plug.logic;
        }
        return gate;
    }

    /** @return The number of rows of trigger/action pairs. */
    public int getRowCount() {
        int slots = gate.variant.numSlots();
        return gate.isSplitInTwo() ? (slots + 1) / 2 : slots;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && tile.getPluggable(side) instanceof PluggableGate;
    }

    // Client -> server

    public void requestValidStatements() {
        sendMessage(ID_VALID_STATEMENTS, buffer -> {});
    }

    public void setConnected(int index, boolean to) {
        sendMessage(ID_CONNECTION, buffer -> {
            buffer.writeByte(index);
            buffer.writeBoolean(to);
        });
    }

    /** Changes a statement on the client (straight away, so that the GUI reacts), and asks the server to do the same.
     *
     * @param params The new parameters (only the ones that fit the statement are kept). */
    public void setAction(int slot, @Nullable ActionWrapper action,
        IStatementParameter[] params) {
        FullStatement<ActionWrapper> full = getGate().statements[slot].action;
        full.set(action, params);
        sendMessage(ID_SET_ACTION, buffer -> {
            buffer.writeByte(slot);
            full.writeToBuffer(buffer);
        });
    }

    public void setTrigger(int slot, @Nullable TriggerWrapper trigger, IStatementParameter[] params) {
        FullStatement<TriggerWrapper> full = getGate().statements[slot].trigger;
        full.set(trigger, params);
        sendMessage(ID_SET_TRIGGER, buffer -> {
            buffer.writeByte(slot);
            full.writeToBuffer(buffer);
        });
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        GateLogic gate = getGate();
        if (side == LogicalSide.SERVER) {
            if (id == ID_CONNECTION) {
                gate.setConnected(buffer.readUnsignedByte(), buffer.readBoolean());
            } else if (id == ID_VALID_STATEMENTS) {
                sendMessage(ID_VALID_STATEMENTS);
            } else if (id == ID_SET_TRIGGER || id == ID_SET_ACTION) {
                readStatementChange(gate, id == ID_SET_ACTION, buffer);
            }
        } else if (id == ID_VALID_STATEMENTS) {
            possibleTriggers.clear();
            possibleActions.clear();
            int numTriggers = buffer.readVarInt();
            int numActions = buffer.readVarInt();
            for (int i = 0; i < numTriggers; i++) {
                TriggerWrapper wrapper = TriggerType.INSTANCE.readFromBuffer(buffer);
                if (gate.isValidTrigger(wrapper)) {
                    possibleTriggers.add(wrapper);
                }
            }
            for (int i = 0; i < numActions; i++) {
                ActionWrapper wrapper = ActionType.INSTANCE.readFromBuffer(buffer);
                if (gate.isValidAction(wrapper)) {
                    possibleActions.add(wrapper);
                }
            }
        }
    }

    private static void readStatementChange(GateLogic gate, boolean isAction, FriendlyByteBuf buffer) {
        int slot = buffer.readUnsignedByte();
        if (slot >= gate.statements.length) {
            return;
        }
        int maxParams = isAction ? gate.variant.numActionArgs() : gate.variant.numTriggerArgs();
        try {
            if (!buffer.readBoolean()) {
                if (isAction) {
                    gate.setAction(slot, null, new IStatementParameter[0]);
                } else {
                    gate.setTrigger(slot, null, new IStatementParameter[0]);
                }
                return;
            }
            Object statement = isAction ? ActionType.INSTANCE.readFromBuffer(buffer)
                : TriggerType.INSTANCE.readFromBuffer(buffer);
            IStatementParameter[] params = new IStatementParameter[maxParams];
            for (int p = 0; p < maxParams; p++) {
                params[p] = StatementTypeParam.INSTANCE.readFromBuffer(buffer);
            }
            if (statement == null) {
                return;
            }
            if (isAction) {
                gate.setAction(slot, (ActionWrapper) statement, params);
            } else {
                gate.setTrigger(slot, (TriggerWrapper) statement, params);
            }
        } catch (RuntimeException e) {
            BCLog.logger.warn("[silicon.gate] Ignored a bad statement change: {}", e.toString());
        }
    }

    @Override
    public void writeMessage(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writeMessage(id, buffer, side);
        if (side == LogicalSide.SERVER && id == ID_VALID_STATEMENTS) {
            GateLogic gate = getGate();
            List<TriggerWrapper> triggers = new ArrayList<>(gate.getAllValidTriggers());
            List<ActionWrapper> actions = new ArrayList<>(gate.getAllValidActions());
            buffer.writeVarInt(triggers.size());
            buffer.writeVarInt(actions.size());
            for (TriggerWrapper wrapper : triggers) {
                TriggerType.INSTANCE.writeToBuffer(buffer, wrapper);
            }
            for (ActionWrapper wrapper : actions) {
                ActionType.INSTANCE.writeToBuffer(buffer, wrapper);
            }
        }
    }

    /** @return The possible statements, grouped by the side they come from (the internal ones first). */
    public static <T extends StatementWrapper> List<List<T>> group(SortedSet<T> from) {
        List<List<T>> groups = new ArrayList<>();
        List<EnumPipePart> parts = new ArrayList<>();
        parts.add(EnumPipePart.CENTER);
        parts.addAll(List.of(EnumPipePart.FACES));
        for (EnumPipePart part : parts) {
            List<T> group = new ArrayList<>();
            for (T value : from) {
                if (value.sourcePart == part) {
                    group.add(value);
                }
            }
            if (!group.isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }
}
