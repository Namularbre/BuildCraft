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
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.ITrigger;
import buildcraft.api.statements.StatementManager;

import buildcraft.lib.statement.StatementType;
import buildcraft.lib.statement.TriggerWrapper;

/** Saves and sends the triggers of gates: their tag, and the side they come from. */
public class TriggerType extends StatementType<TriggerWrapper> {
    public static final TriggerType INSTANCE = new TriggerType();

    private TriggerType() {
        super(TriggerWrapper.class, null);
    }

    static TriggerWrapper wrap(String kind, EnumPipePart side) {
        IStatement statement = StatementManager.statements.get(kind);
        if (statement instanceof ITrigger) {
            try {
                return TriggerWrapper.wrap(statement, side.face);
            } catch (RuntimeException e) {
                // Like an internal trigger that came with a side (or the reverse)
                BCLog.logger.warn("[silicon.gate] Couldn't use the trigger '{}' on the side {}", kind, side);
                return null;
            }
        }
        BCLog.logger.warn("[silicon.gate] Couldn't find a trigger called '{}'! (found {})", kind, statement);
        return null;
    }

    @Override
    public TriggerWrapper readFromNbt(CompoundTag nbt) {
        String kind = nbt.getString("kind");
        if (kind.isEmpty()) {
            return null;
        }
        return wrap(kind, EnumPipePart.fromMeta(nbt.getByte("side")));
    }

    @Override
    public CompoundTag writeToNbt(TriggerWrapper slot) {
        CompoundTag nbt = new CompoundTag();
        if (slot != null) {
            nbt.putString("kind", slot.getUniqueTag());
            nbt.putByte("side", (byte) slot.sourcePart.getIndex());
        }
        return nbt;
    }

    @Override
    public TriggerWrapper readFromBuffer(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            String name = buffer.readUtf();
            EnumPipePart part = buffer.readEnum(EnumPipePart.class);
            return wrap(name, part);
        }
        return null;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, TriggerWrapper slot) {
        if (slot == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeUtf(slot.getUniqueTag());
            buffer.writeEnum(slot.sourcePart);
        }
    }
}
