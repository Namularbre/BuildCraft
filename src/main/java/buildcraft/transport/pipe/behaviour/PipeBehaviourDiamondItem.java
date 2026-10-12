/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeEventItem.ItemEntry;

/** Sends items to the sides whose filters match them. A side without filters takes what no other side wants. When
 * several sides match, items are shared between them by how many of each matching filter they have. */
public class PipeBehaviourDiamondItem extends PipeBehaviourDiamond {
    public PipeBehaviourDiamondItem(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDiamondItem(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
    }

    private static boolean matches(ItemStack filter, ItemStack stack) {
        return ItemStack.isSameItemSameTags(filter, stack);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        ItemStack toCompare = sideCheck.stack;
        for (Direction face : Direction.values()) {
            if (!sideCheck.isAllowed(face) || !pipe.isConnected(face)) {
                continue;
            }
            int offset = FILTERS_PER_SIDE * face.ordinal();
            boolean hasFilter = false;
            boolean matched = false;
            for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                ItemStack filter = filters.getStackInSlot(offset + i);
                if (!filter.isEmpty()) {
                    hasFilter = true;
                    if (matches(filter, toCompare)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                sideCheck.increasePriority(face, 12);
            } else if (hasFilter) {
                sideCheck.disallow(face);
            }
        }
    }

    /** Splits each stack between the possible sides, in proportion to the number of matching filter items on each
     * side (or evenly if no filter matches). */
    @PipeEventHandler
    public void split(PipeEventItem.Split split) {
        Direction[] allSides = split.getAllPossibleDestinations().toArray(new Direction[0]);
        if (allSides.length <= 1) {
            return;
        }
        List<ItemEntry> items = new ArrayList<>(split.items);
        split.items.clear();
        for (ItemEntry item : items) {
            int[] countPerSide = new int[allSides.length];
            int totalCount = 0;
            for (int s = 0; s < allSides.length; s++) {
                int offset = FILTERS_PER_SIDE * allSides[s].ordinal();
                for (int i = 0; i < FILTERS_PER_SIDE; i++) {
                    ItemStack filter = filters.getStackInSlot(offset + i);
                    if (!filter.isEmpty() && matches(filter, item.stack)) {
                        totalCount += filter.getCount();
                        countPerSide[s] += filter.getCount();
                    }
                }
            }
            if (totalCount == 0) {
                totalCount = allSides.length;
                Arrays.fill(countPerSide, 1);
            }

            ItemEntry[] entries = new ItemEntry[allSides.length];
            ItemStack toSplit = item.stack.copy();
            // Whole multiples of the ratio are split exactly
            int multiples = toSplit.getCount() / totalCount;
            if (multiples > 0) {
                for (int s = 0; s < allSides.length; s++) {
                    if (countPerSide[s] > 0) {
                        entries[s] = entry(item, toSplit.copyWithCount(countPerSide[s] * multiples), allSides[s]);
                    }
                }
                toSplit.shrink(multiples * totalCount);
            }
            // The rest goes to random sides, weighted by the ratio
            while (!toSplit.isEmpty()) {
                int rand = split.holder.getPipeWorld().random.nextInt(totalCount);
                int side = 0;
                while (rand >= countPerSide[side]) {
                    rand -= countPerSide[side];
                    side++;
                }
                if (entries[side] == null) {
                    entries[side] = entry(item, toSplit.copyWithCount(1), allSides[side]);
                } else {
                    entries[side].stack.grow(1);
                }
                toSplit.shrink(1);
            }
            for (ItemEntry entry : entries) {
                if (entry != null) {
                    split.items.add(entry);
                }
            }
        }
    }

    private static ItemEntry entry(ItemEntry from, ItemStack stack, Direction to) {
        ItemEntry entry = new ItemEntry(from.colour, stack, from.from);
        entry.to = new ArrayList<>(List.of(to));
        return entry;
    }
}
