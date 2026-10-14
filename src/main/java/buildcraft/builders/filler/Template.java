/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.BitSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

import buildcraft.api.filler.IFilledTemplate;

/** Which blocks of a box should be filled, as a bit per block. */
public class Template implements IFilledTemplate {
    private final BlockPos size;
    private final BlockPos max;
    public final BitSet data;
    /** Where the template starts in the world, or null if it starts at the bottom corner of the box of the filler
     * (only patterns that go outside of the box set it). */
    @Nullable
    public final BlockPos min;

    public Template(BlockPos size) {
        this(null, size);
    }

    public Template(@Nullable BlockPos min, BlockPos size) {
        this.min = min;
        this.size = size;
        this.max = size.offset(-1, -1, -1);
        this.data = new BitSet(getDataSize(size));
    }

    public static int getDataSize(BlockPos size) {
        return size.getX() * size.getY() * size.getZ();
    }

    public static int posToIndex(BlockPos size, int x, int y, int z) {
        return x + size.getX() * (y + size.getY() * z);
    }

    public int posToIndex(int x, int y, int z) {
        return posToIndex(size, x, y, z);
    }

    public BlockPos indexToPos(int index) {
        int x = index % size.getX();
        int rest = index / size.getX();
        return new BlockPos(x, rest % size.getY(), rest / size.getY());
    }

    public void invert() {
        data.flip(0, getDataSize(size));
    }

    private void checkPos(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= size.getX() || y >= size.getY() || z >= size.getZ()) {
            throw new IllegalArgumentException("Size: " + size + ", pos: " + new BlockPos(x, y, z));
        }
    }

    @Override
    public BlockPos getSize() {
        return size;
    }

    @Override
    public BlockPos getMax() {
        return max;
    }

    @Override
    public void set(int x, int y, int z, boolean value) {
        checkPos(x, y, z);
        data.set(posToIndex(x, y, z), value);
    }

    @Override
    public boolean get(int x, int y, int z) {
        checkPos(x, y, z);
        return data.get(posToIndex(x, y, z));
    }

    @Override
    public void setLineX(int fromX, int toX, int y, int z, boolean value) {
        checkPos(fromX, y, z);
        checkPos(toX, y, z);
        data.set(posToIndex(fromX, y, z), posToIndex(toX, y, z) + 1, value);
    }

    @Override
    public void setPlaneXY(int z, boolean value) {
        checkPos(0, 0, z);
        data.set(posToIndex(0, 0, z), posToIndex(max.getX(), max.getY(), z) + 1, value);
    }

    @Override
    public void setAll(boolean value) {
        data.set(0, getDataSize(size), value);
    }
}
