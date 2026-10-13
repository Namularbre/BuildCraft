/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Every block of the box (but not what is inside them, like the items in chests). */
public class Blueprint extends Snapshot {
    /** The different states, used by {@link #data}. */
    public final List<BlockState> palette = new ArrayList<>();
    /** The index in {@link #palette} of each block. */
    public int[] data = new int[0];

    @Override
    public Type getType() {
        return Type.BLUEPRINT;
    }

    /** @return The state that a blueprint keeps for the given one (air for things that can't be built, like
     *         fluids and fire). */
    public static BlockState toScanned(BlockState state) {
        if (state.getBlock() instanceof LiquidBlock || getRequiredItem(state).isEmpty() && !isFreeHalf(state)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** @return True if the state is the second half of something (like the top of a door), which is free. */
    private static boolean isFreeHalf(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            || state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
    }

    /** @return The items needed to place the given state (empty if it's free, or can't be placed). */
    public static ItemStack getRequiredItem(BlockState state) {
        if (state.isAir() || isFreeHalf(state)) {
            return ItemStack.EMPTY;
        }
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        int count = 1;
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
            && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            count = 2;
        }
        return new ItemStack(item, count);
    }

    @Override
    protected void writeData(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (BlockState state : palette) {
            list.add(NbtUtils.writeBlockState(state));
        }
        nbt.put("palette", list);
        nbt.putIntArray("data", data);
    }

    @Override
    protected void readData(CompoundTag nbt) {
        palette.clear();
        ListTag list = nbt.getList("palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            palette.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), list.getCompound(i)));
        }
        int[] read = nbt.getIntArray("data");
        data = read.length == getDataSize() ? read : new int[getDataSize()];
        if (palette.isEmpty()) {
            palette.add(Blocks.AIR.defaultBlockState());
        }
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0 || data[i] >= palette.size()) {
                data[i] = 0;
            }
        }
    }
}
