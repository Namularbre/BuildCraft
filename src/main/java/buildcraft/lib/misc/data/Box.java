/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc.data;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;

/** A box of blocks, from min to max (both included). */
public record Box(BlockPos min, BlockPos max) {
    public Box {
        BlockPos realMin = new BlockPos(
            Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ())
        );
        BlockPos realMax = new BlockPos(
            Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ())
        );
        min = realMin;
        max = realMax;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY()
            && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /** @return True if the position is on one of the 12 edges of the box. */
    public boolean isOnEdge(BlockPos pos) {
        if (!contains(pos)) {
            return false;
        }
        int onBounds = 0;
        if (pos.getX() == min.getX() || pos.getX() == max.getX()) onBounds++;
        if (pos.getY() == min.getY() || pos.getY() == max.getY()) onBounds++;
        if (pos.getZ() == min.getZ() || pos.getZ() == max.getZ()) onBounds++;
        return onBounds >= 2;
    }

    public List<BlockPos> getBlocksInArea() {
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            list.add(pos.immutable());
        }
        return list;
    }

    /** @return The position in the box nearest to the given one. */
    public BlockPos closestInsideTo(BlockPos pos) {
        return new BlockPos(
            Math.max(min.getX(), Math.min(max.getX(), pos.getX())),
            Math.max(min.getY(), Math.min(max.getY(), pos.getY())),
            Math.max(min.getZ(), Math.min(max.getZ(), pos.getZ()))
        );
    }

    /** @return The box covering every block of this box. */
    public AABB toAABB() {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("min", NbtUtils.writeBlockPos(min));
        nbt.put("max", NbtUtils.writeBlockPos(max));
        return nbt;
    }

    @Nullable
    public static Box load(CompoundTag nbt) {
        if (!nbt.contains("min") || !nbt.contains("max")) {
            return null;
        }
        return new Box(NbtUtils.readBlockPos(nbt.getCompound("min")), NbtUtils.readBlockPos(nbt.getCompound("max")));
    }

    public static void write(FriendlyByteBuf buffer, @Nullable Box box) {
        buffer.writeBoolean(box != null);
        if (box != null) {
            buffer.writeBlockPos(box.min);
            buffer.writeBlockPos(box.max);
        }
    }

    @Nullable
    public static Box read(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? new Box(buffer.readBlockPos(), buffer.readBlockPos()) : null;
    }
}
