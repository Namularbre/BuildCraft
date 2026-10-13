/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.statements.StatementManager.IParamReaderBuf;
import buildcraft.api.statements.StatementManager.IParameterReader;

public class StatementTypeParam extends StatementType<IStatementParameter> {
    public static final StatementTypeParam INSTANCE = new StatementTypeParam();

    public StatementTypeParam() {
        super(IStatementParameter.class, null);
    }

    @Override
    public IStatementParameter readFromNbt(CompoundTag nbt) {
        IParameterReader reader = StatementManager.getParameterReader(nbt.getString("kind"));
        return reader == null ? null : reader.readFromNbt(nbt);
    }

    @Override
    public CompoundTag writeToNbt(IStatementParameter slot) {
        CompoundTag nbt = new CompoundTag();
        if (slot != null) {
            slot.writeToNbt(nbt);
            nbt.putString("kind", slot.getUniqueTag());
        }
        return nbt;
    }

    @Override
    public IStatementParameter readFromBuffer(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            String tag = buffer.readUtf();
            IParamReaderBuf reader = StatementManager.paramsBuf.get(tag);
            if (reader == null) {
                // Unlike unknown statements, the rest of the buffer can't be read without the reader
                throw new IllegalStateException("Unknown parameter type " + tag);
            }
            return reader.readFromBuf(buffer);
        } else {
            return null;
        }
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, IStatementParameter slot) {
        if (slot == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeUtf(slot.getUniqueTag());
            slot.writeToBuf(buffer);
        }
    }
}
