/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.BitSet;

import net.minecraft.nbt.CompoundTag;

/** Only which blocks of the box are filled. */
public class SnapshotTemplate extends Snapshot {
    public BitSet data = new BitSet();

    @Override
    public Type getType() {
        return Type.TEMPLATE;
    }

    @Override
    protected void writeData(CompoundTag nbt) {
        nbt.putByteArray("data", data.toByteArray());
    }

    @Override
    protected void readData(CompoundTag nbt) {
        data = BitSet.valueOf(nbt.getByteArray("data"));
    }
}
