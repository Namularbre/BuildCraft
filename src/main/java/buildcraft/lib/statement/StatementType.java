/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import buildcraft.api.statements.IGuiSlot;

/** Saves, loads and sends one kind of {@link IGuiSlot} (triggers, actions or parameters). */
public abstract class StatementType<S extends IGuiSlot> {
    public final Class<S> clazz;
    public final S defaultStatement;

    public StatementType(Class<S> clazz, S defaultStatement) {
        this.clazz = clazz;
        this.defaultStatement = defaultStatement;
    }

    /** Reads a slot from the given tag, which will be equal to the one returned by {@link #writeToNbt(IGuiSlot)}.
     *
     * @return The slot, or null if it couldn't be read. */
    @Nullable
    public abstract S readFromNbt(CompoundTag nbt);

    public abstract CompoundTag writeToNbt(S slot);

    /** Reads a slot from the given buffer, as written by {@link #writeToBuffer(FriendlyByteBuf, IGuiSlot)}.
     *
     * @return The slot, or null if it couldn't be read. */
    @Nullable
    public abstract S readFromBuffer(FriendlyByteBuf buffer);

    public abstract void writeToBuffer(FriendlyByteBuf buffer, S slot);
}
