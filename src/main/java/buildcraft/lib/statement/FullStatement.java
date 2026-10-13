/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;

/** Util class for holding, saving, loading and networking a {@link StatementWrapper} and its
 * {@link IStatementParameter}'s. */
public class FullStatement<S extends IStatement> {
    public final StatementType<S> type;
    public final int maxParams;
    private final IStatementParameter[] params;
    private S statement;

    public FullStatement(StatementType<S> type, int maxParams) {
        this.type = type;
        this.statement = type.defaultStatement;
        this.maxParams = maxParams;
        this.params = new IStatementParameter[maxParams];
    }

    // NBT

    public void readFromNbt(CompoundTag nbt) {
        statement = nbt.contains("s") ? type.readFromNbt(nbt.getCompound("s")) : null;
        Arrays.fill(params, null);
        if (statement != null) {
            for (int p = 0; p < params.length; p++) {
                CompoundTag paramTag = nbt.getCompound(Integer.toString(p));
                IStatementParameter param = StatementTypeParam.INSTANCE.readFromNbt(paramTag);
                if (param == null) {
                    // Fall back to the default parameter, rather than leaving an empty one
                    param = statement.createParameter(null, p);
                }
                params[p] = param;
            }
        }
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        if (statement != null) {
            nbt.put("s", type.writeToNbt(statement));
            for (int p = 0; p < params.length; p++) {
                IStatementParameter param = params[p];
                if (param != null) {
                    nbt.put(Integer.toString(p), StatementTypeParam.INSTANCE.writeToNbt(param));
                }
            }
        }
        return nbt;
    }

    // Networking

    public void readFromBuffer(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            statement = type.readFromBuffer(buffer);
            for (int p = 0; p < params.length; p++) {
                params[p] = StatementTypeParam.INSTANCE.readFromBuffer(buffer);
            }
            if (statement == null) {
                Arrays.fill(params, null);
            }
        } else {
            statement = type.defaultStatement;
            Arrays.fill(params, null);
        }
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
        if (statement == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            type.writeToBuffer(buffer, statement);
            for (IStatementParameter param : params) {
                StatementTypeParam.INSTANCE.writeToBuffer(buffer, param);
            }
        }
    }

    // Statement

    @Nullable
    public S get() {
        return statement;
    }

    /** Changes the statement, keeping the parameters that still fit it (and creating the missing ones). */
    public void set(@Nullable S to) {
        statement = to;
        if (statement == null) {
            Arrays.fill(params, null);
            return;
        }
        for (int i = 0; i < params.length; i++) {
            if (i >= statement.maxParameters()) {
                params[i] = null;
            } else {
                params[i] = statement.createParameter(params[i], i);
            }
        }
    }

    public boolean canSet(@Nullable S value) {
        return value == null || value.minParameters() <= params.length;
    }

    /** Changes the statement and its parameters, keeping only the parameters that the statement accepts. */
    public void set(@Nullable S to, IStatementParameter[] newParams) {
        set(to);
        for (int i = Math.min(params.length, newParams.length) - 1; i >= 0; i--) {
            if (canSet(i, newParams[i])) {
                params[i] = newParams[i];
            }
        }
    }

    // Params

    @Nullable
    public IStatementParameter get(int index) {
        return params[index];
    }

    public void set(int index, @Nullable IStatementParameter param) {
        params[index] = param;
    }

    /** @return True if the statement would keep the given parameter at the given index. */
    public boolean canSet(int index, @Nullable IStatementParameter param) {
        if (statement == null || param == null || index >= statement.maxParameters()) {
            return false;
        }
        return statement.createParameter(param, index) == param;
    }

    public int getParamCount() {
        return params.length;
    }

    public IStatementParameter[] getParameters() {
        return params;
    }
}
