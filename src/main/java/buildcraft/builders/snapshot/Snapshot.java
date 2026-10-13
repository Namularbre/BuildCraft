/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;

/** A scan of a box of the world: a template (only which blocks are there) or a blueprint (every block). */
public abstract class Snapshot {
    public enum Type {
        TEMPLATE,
        BLUEPRINT
    }

    public BlockPos size = BlockPos.ZERO;
    /** The way the architect table that scanned this was facing. */
    public Direction facing = Direction.NORTH;
    /** Where the box was, from the block behind the architect table (where builders build from). */
    public BlockPos offset = BlockPos.ZERO;

    public abstract Type getType();

    protected abstract void writeData(CompoundTag nbt);

    protected abstract void readData(CompoundTag nbt);

    public int getDataSize() {
        return size.getX() * size.getY() * size.getZ();
    }

    public static CompoundTag write(Snapshot snapshot) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", snapshot.getType().name());
        nbt.put("size", NbtUtils.writeBlockPos(snapshot.size));
        nbt.putString("facing", snapshot.facing.getName());
        nbt.put("offset", NbtUtils.writeBlockPos(snapshot.offset));
        snapshot.writeData(nbt);
        return nbt;
    }

    public static Snapshot read(CompoundTag nbt) {
        Type type = Type.valueOf(nbt.getString("type"));
        Snapshot snapshot = type == Type.TEMPLATE ? new SnapshotTemplate() : new Blueprint();
        snapshot.size = NbtUtils.readBlockPos(nbt.getCompound("size"));
        Direction facing = Direction.byName(nbt.getString("facing"));
        snapshot.facing = facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing;
        snapshot.offset = NbtUtils.readBlockPos(nbt.getCompound("offset"));
        snapshot.readData(nbt);
        return snapshot;
    }

    /** @return The hash of this snapshot (so that the same snapshot is only stored once). */
    public String computeKey() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.write(write(this), new DataOutputStream(bytes));
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
            return HexFormat.of().formatHex(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Couldn't hash a snapshot", e);
        }
    }
}
