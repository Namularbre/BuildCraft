/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import buildcraft.api.filler.FillerManager;
import buildcraft.api.filler.IFillerPattern;

import buildcraft.lib.statement.StatementType;

/** Saves and sends filler patterns (by their tag). Unknown patterns become the "none" pattern. */
public class FillerType extends StatementType<IFillerPattern> {
    public static final FillerType INSTANCE = new FillerType();

    private FillerType() {
        super(IFillerPattern.class, BCBuildersStatements.PATTERN_NONE);
    }

    private IFillerPattern get(String kind) {
        IFillerPattern pattern = FillerManager.registry.getPattern(kind);
        return pattern == null ? defaultStatement : pattern;
    }

    @Override
    public IFillerPattern readFromNbt(CompoundTag nbt) {
        return get(nbt.getString("kind"));
    }

    @Override
    public CompoundTag writeToNbt(IFillerPattern slot) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("kind", slot.getUniqueTag());
        return nbt;
    }

    @Override
    public IFillerPattern readFromBuffer(FriendlyByteBuf buffer) {
        return get(buffer.readUtf());
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buffer, IFillerPattern slot) {
        buffer.writeUtf(slot.getUniqueTag());
    }
}
