/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IAction;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementManager;

import buildcraft.lib.statement.ActionWrapper;
import buildcraft.lib.statement.StatementType;

/** Saves and sends the actions of gates: their tag, and the side they come from. */
public class ActionType extends StatementType<ActionWrapper> {
    public static final ActionType INSTANCE = new ActionType();

    private ActionType() {
        super(ActionWrapper.class, null);
    }

    static ActionWrapper wrap(String kind, EnumPipePart side) {
        IStatement statement = StatementManager.statements.get(kind);
        if (statement instanceof IAction) {
            try {
                return ActionWrapper.wrap(statement, side.face);
            } catch (RuntimeException e) {
                // Like an internal action that came with a side (or the reverse)
                BCLog.logger.warn("[silicon.gate] Couldn't use the action '{}' on the side {}", kind, side);
                return null;
            }
        }
        BCLog.logger.warn("[silicon.gate] Couldn't find an action called '{}'! (found {})", kind, statement);
        return null;
    }

    @Override
    public ActionWrapper readFromNbt(CompoundTag nbt) {
        String kind = nbt.getString("kind");
        if (kind.isEmpty()) {
            return null;
        }
        return wrap(kind, EnumPipePart.fromMeta(nbt.getByte("side")));
    }

    @Override
    public CompoundTag writeToNbt(ActionWrapper slot) {
        CompoundTag nbt = new CompoundTag();
        if (slot != null) {
            nbt.putString("kind", slot.getUniqueTag());
            nbt.putByte("side", (byte) slot.sourcePart.getIndex());
        }
        return nbt;
    }

    @Override
    public ActionWrapper readFromBuffer(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            String name = buffer.readUtf();
            EnumPipePart part = buffer.readEnum(EnumPipePart.class);
            return wrap(name, part);
        }
        return null;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, ActionWrapper slot) {
        if (slot == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeUtf(slot.getUniqueTag());
            buffer.writeEnum(slot.sourcePart);
        }
    }
}
