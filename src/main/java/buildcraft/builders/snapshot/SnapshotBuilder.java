/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;

import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.data.Box;

import buildcraft.factory.tile.TileMiningWell;

/** Makes the blocks of a box match a {@link BuildTarget}: the blocks that shouldn't be there are broken (if the
 * machine can excavate), then blocks from the machine's inventory are placed where they should be. */
public class SnapshotBuilder {
    private static final int MAX_QUEUE_SIZE = 16;
    private static final int CHECKS_PER_TICK = 64;
    private static final long MAX_POWER_PER_TICK = 256 * MjAPI.MJ;

    private final ISnapshotBuilderTile tile;
    @Nullable
    private BuildTarget target;
    private BuildTarget.Result[] results;
    private int[] breakOrder;
    private int[] placeOrder;
    private int checkIndex;
    /** True once every block of the box was checked at least once. */
    private boolean checkedAll;

    public final List<Task> breakTasks = new ArrayList<>();
    public final List<Task> placeTasks = new ArrayList<>();
    public int leftToBreak;
    public int leftToPlace;

    public SnapshotBuilder(ISnapshotBuilderTile tile) {
        this.tile = tile;
    }

    /** A block being broken or placed, and the power put into it so far. */
    public static class Task {
        public final BlockPos pos;
        /** The items used to place the block (empty for break tasks, or for free blocks). */
        public final ItemStack item;
        public long power;
        public long target;

        public Task(BlockPos pos, ItemStack item, long target) {
            this.pos = pos;
            this.item = item;
            this.target = Math.max(1, target);
        }

        public double getProgress() {
            return Math.min(1, power / (double) target);
        }
    }

    /** Changes what to build (or stops building, if null). */
    public void setTarget(@Nullable BuildTarget target) {
        cancel();
        this.target = target;
        if (target == null) {
            results = null;
            return;
        }
        int size = target.count();
        results = new BuildTarget.Result[size];
        Box box = target.box;
        BlockPos center = new BlockPos((box.min().getX() + box.max().getX()) / 2, box.min().getY(),
            (box.min().getZ() + box.max().getZ()) / 2);
        BlockPos machine = tile.getBlockPos();
        // Break from the top down, place from the bottom up (both starting near the middle / the machine)
        breakOrder = IntStream.range(0, size).boxed()
            .sorted(Comparator.comparingInt((Integer i) -> -target.worldPos(i).getY())
                .thenComparingDouble(i -> target.worldPos(i).distSqr(center)))
            .mapToInt(i -> i).toArray();
        placeOrder = IntStream.range(0, size).boxed()
            .sorted(Comparator.comparingInt((Integer i) -> target.worldPos(i).getY())
                .thenComparingDouble(i -> target.worldPos(i).distSqr(machine)))
            .mapToInt(i -> i).toArray();
        checkIndex = 0;
        checkedAll = false;
    }

    @Nullable
    public BuildTarget getTarget() {
        return target;
    }

    /** @return True if every block of the box is right (or there is nothing to build). */
    public boolean isFinished() {
        return results == null || (checkedAll && leftToBreak == 0 && leftToPlace == 0 && breakTasks.isEmpty()
            && placeTasks.isEmpty());
    }

    /** Stops every task, giving back the power and the items that they took. */
    public void cancel() {
        for (Task task : breakTasks) {
            refund(task);
        }
        for (Task task : placeTasks) {
            refund(task);
        }
        breakTasks.clear();
        placeTasks.clear();
        leftToBreak = 0;
        leftToPlace = 0;
    }

    private void refund(Task task) {
        MjBattery battery = tile.getBattery();
        battery.addPower(Math.min(task.power, battery.getCapacity() - battery.getStored()), false);
        if (!task.item.isEmpty()) {
            tile.returnItem(task.item);
        }
    }

    private void check(int index) {
        results[index] = target.check(tile.getLevel(), index, target.worldPos(index));
    }

    /** @return The items that are still needed to finish (for blueprints, as templates take any block). */
    public List<ItemStack> getRemainingRequiredItems() {
        List<ItemStack> all = new ArrayList<>();
        if (results == null) {
            return all;
        }
        for (int i = 0; i < results.length; i++) {
            if (results[i] != BuildTarget.Result.TO_PLACE) {
                continue;
            }
            for (ItemStack required : target.getRequiredItems(i)) {
                boolean merged = false;
                for (ItemStack stack : all) {
                    if (ItemStack.isSameItemSameTags(stack, required)) {
                        stack.grow(required.getCount());
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    all.add(required.copy());
                }
            }
        }
        return all;
    }

    /** Called every tick on the server. */
    public void tick() {
        if (results == null) {
            return;
        }
        for (int i = 0; i < CHECKS_PER_TICK && i < results.length; i++) {
            check(checkIndex);
            checkIndex++;
            if (checkIndex >= results.length) {
                checkIndex = 0;
                checkedAll = true;
            }
        }
        // Forget the tasks that aren't needed anymore (like a block placed by a player)
        removeDone(breakTasks, BuildTarget.Result.TO_BREAK);
        removeDone(placeTasks, BuildTarget.Result.TO_PLACE);

        leftToBreak = 0;
        leftToPlace = 0;
        for (BuildTarget.Result result : results) {
            if (result == BuildTarget.Result.TO_BREAK) {
                leftToBreak++;
            } else if (result == BuildTarget.Result.TO_PLACE) {
                leftToPlace++;
            }
        }
        ServerLevel level = (ServerLevel) tile.getLevel();
        if (tile.canExcavate()) {
            for (int index : breakOrder) {
                if (breakTasks.size() >= MAX_QUEUE_SIZE) {
                    break;
                }
                BlockPos pos = target.worldPos(index);
                if (results[index] == BuildTarget.Result.TO_BREAK && !isQueued(breakTasks, pos)) {
                    breakTasks.add(new Task(pos, ItemStack.EMPTY, TileMiningWell.getBreakPower(level, pos)));
                }
            }
        }
        if (breakTasks.isEmpty()) {
            for (int index : placeOrder) {
                if (placeTasks.size() >= MAX_QUEUE_SIZE) {
                    break;
                }
                BlockPos pos = target.worldPos(index);
                if (results[index] == BuildTarget.Result.TO_PLACE && !isQueued(placeTasks, pos)) {
                    ItemStack items = target.takeItems(tile.getResources(), index);
                    if (items == null) {
                        continue;
                    }
                    double distance = Math.sqrt(pos.distSqr(tile.getBlockPos()));
                    placeTasks.add(new Task(pos, items, (long) (distance * 10 * MjAPI.MJ)));
                }
            }
        }
        doTasks(level);
    }

    private void removeDone(List<Task> tasks, BuildTarget.Result expected) {
        for (Iterator<Task> iterator = tasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            int index = target.indexOf(task.pos);
            if (index < 0 || results[index] != expected) {
                iterator.remove();
                refund(task);
            }
        }
    }

    private static boolean isQueued(List<Task> tasks, BlockPos pos) {
        for (Task task : tasks) {
            if (task.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private void doTasks(ServerLevel level) {
        MjBattery battery = tile.getBattery();
        // Less power is used when the battery is low
        long max = Math.min((long) (MAX_POWER_PER_TICK * (double) (battery.getStored() + MAX_POWER_PER_TICK / 10)
            / (battery.getCapacity() * 2)), MAX_POWER_PER_TICK);
        for (Iterator<Task> iterator = breakTasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            task.power += battery.extractPower(0, Math.min(task.target - task.power, max / breakTasks.size()));
            if (task.power >= task.target) {
                level.destroyBlockProgress(task.pos.hashCode(), task.pos, -1);
                // Like in 1.12, what is broken is lost
                if (BlockUtil.breakBlockAndGetDrops(level, task.pos, tile.getOwner()).isEmpty()) {
                    refund(task);
                }
                check(target.indexOf(task.pos));
                iterator.remove();
            } else {
                level.destroyBlockProgress(task.pos.hashCode(), task.pos, (int) (task.power * 9 / task.target));
            }
        }
        for (Iterator<Task> iterator = placeTasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            task.power += battery.extractPower(0, Math.min(task.target - task.power, max / placeTasks.size()));
            if (task.power >= task.target) {
                int index = target.indexOf(task.pos);
                if (!target.place(level, index, task.pos, task.item, tile.getOwner()) && !task.item.isEmpty()) {
                    tile.returnItem(task.item);
                }
                check(index);
                iterator.remove();
            }
        }
    }

    // Networking (only the tasks, for rendering)

    public void writeTasks(FriendlyByteBuf buffer) {
        buffer.writeVarInt(breakTasks.size());
        for (Task task : breakTasks) {
            buffer.writeBlockPos(task.pos);
            buffer.writeFloat((float) task.getProgress());
        }
        buffer.writeVarInt(placeTasks.size());
        for (Task task : placeTasks) {
            buffer.writeBlockPos(task.pos);
            buffer.writeFloat((float) task.getProgress());
            buffer.writeItem(task.item);
        }
        buffer.writeVarInt(leftToBreak);
        buffer.writeVarInt(leftToPlace);
    }

    public void readTasks(FriendlyByteBuf buffer) {
        breakTasks.clear();
        placeTasks.clear();
        int breaks = buffer.readVarInt();
        for (int i = 0; i < breaks; i++) {
            Task task = new Task(buffer.readBlockPos(), ItemStack.EMPTY, 1000);
            task.power = (long) (buffer.readFloat() * 1000);
            breakTasks.add(task);
        }
        int places = buffer.readVarInt();
        for (int i = 0; i < places; i++) {
            BlockPos pos = buffer.readBlockPos();
            float progress = buffer.readFloat();
            Task task = new Task(pos, buffer.readItem(), 1000);
            task.power = (long) (progress * 1000);
            placeTasks.add(task);
        }
        leftToBreak = buffer.readVarInt();
        leftToPlace = buffer.readVarInt();
    }

    /** @return The items of the place tasks (to save them, as tasks aren't saved). */
    public List<ItemStack> getTaskItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Task task : placeTasks) {
            if (!task.item.isEmpty()) {
                items.add(task.item);
            }
        }
        return items;
    }
}
