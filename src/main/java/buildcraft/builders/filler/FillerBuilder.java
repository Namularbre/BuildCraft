/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;

import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.data.Box;

import buildcraft.builders.tile.TileFiller;
import buildcraft.factory.tile.TileMiningWell;

/** Makes the blocks of a box match a template: the blocks that shouldn't be there are broken (if the filler can
 * excavate), then blocks from the filler's inventory are placed where they should be. */
public class FillerBuilder {
    private static final int MAX_QUEUE_SIZE = 16;
    private static final int CHECKS_PER_TICK = 64;
    private static final long MAX_POWER_PER_TICK = 256 * MjAPI.MJ;

    private static final byte UNKNOWN = 0, CORRECT = 1, TO_BREAK = 2, TO_PLACE = 3;

    private final TileFiller tile;
    @Nullable
    private Box box;
    @Nullable
    private Template template;
    private byte[] results;
    private int[] breakOrder;
    private int[] placeOrder;
    private int checkIndex;
    /** True once every block of the box was checked at least once. */
    private boolean checkedAll;

    public final List<Task> breakTasks = new ArrayList<>();
    public final List<Task> placeTasks = new ArrayList<>();
    public int leftToBreak;
    public int leftToPlace;

    public FillerBuilder(TileFiller tile) {
        this.tile = tile;
    }

    /** A block being broken or placed, and the power put into it so far. */
    public static class Task {
        public final BlockPos pos;
        /** The block to place (empty for break tasks). */
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

    /** Changes what to build (or stops building, if either is null). */
    public void setTarget(@Nullable Box box, @Nullable Template template) {
        cancel();
        this.box = box;
        this.template = template;
        if (box == null || template == null) {
            results = null;
            return;
        }
        int size = Template.getDataSize(template.getSize());
        results = new byte[size];
        BlockPos min = box.min();
        BlockPos center = min.offset(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
        BlockPos fillerPos = tile.getBlockPos();
        // Break from the top down, place from the bottom up (both starting near the middle / the filler)
        breakOrder = IntStream.range(0, size).boxed().sorted(Comparator.comparingInt((Integer i) -> -pos(i).getY())
            .thenComparingDouble(i -> pos(i).distSqr(center))).mapToInt(i -> i).toArray();
        placeOrder = IntStream.range(0, size).boxed().sorted(Comparator.comparingInt((Integer i) -> pos(i).getY())
            .thenComparingDouble(i -> pos(i).distSqr(fillerPos))).mapToInt(i -> i).toArray();
        checkIndex = 0;
        checkedAll = false;
    }

    public boolean hasTarget() {
        return results != null;
    }

    /** @return True if every block of the box is right. */
    public boolean isFinished() {
        return results == null || (checkedAll && leftToBreak == 0 && leftToPlace == 0 && breakTasks.isEmpty()
            && placeTasks.isEmpty());
    }

    private BlockPos pos(int index) {
        return box.min().offset(template.indexToPos(index));
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

    private byte check(int index) {
        BlockPos pos = pos(index);
        Level level = tile.getLevel();
        BlockState state = level.getBlockState(pos);
        byte result;
        if (template.data.get(index)) {
            // Any block is fine, as long as there is one (fluids get replaced)
            result = state.isAir() || state.getBlock() instanceof LiquidBlock ? TO_PLACE : CORRECT;
        } else if (state.isAir() || state.getBlock() instanceof LiquidBlock || !BlockUtil.canMachineBreak(level, pos)) {
            result = CORRECT;
        } else {
            result = TO_BREAK;
        }
        results[index] = result;
        return result;
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
        removeDone(breakTasks, TO_BREAK);
        removeDone(placeTasks, TO_PLACE);

        leftToBreak = 0;
        leftToPlace = 0;
        for (byte result : results) {
            if (result == TO_BREAK) {
                leftToBreak++;
            } else if (result == TO_PLACE) {
                leftToPlace++;
            }
        }
        if (tile.canExcavate()) {
            for (int index : breakOrder) {
                if (breakTasks.size() >= MAX_QUEUE_SIZE) {
                    break;
                }
                if (results[index] == TO_BREAK && !isQueued(breakTasks, pos(index))) {
                    BlockPos pos = pos(index);
                    breakTasks.add(new Task(pos, ItemStack.EMPTY,
                        TileMiningWell.getBreakPower((ServerLevel) tile.getLevel(), pos)));
                }
            }
        }
        if (breakTasks.isEmpty()) {
            for (int index : placeOrder) {
                if (placeTasks.size() >= MAX_QUEUE_SIZE) {
                    break;
                }
                if (results[index] == TO_PLACE && !isQueued(placeTasks, pos(index))) {
                    ItemStack item = tile.takeBlockItem();
                    if (item.isEmpty()) {
                        break;
                    }
                    BlockPos pos = pos(index);
                    double distance = Math.sqrt(pos.distSqr(tile.getBlockPos()));
                    placeTasks.add(new Task(pos, item, (long) (distance * 10 * MjAPI.MJ)));
                }
            }
        }
        doTasks();
    }

    private void removeDone(List<Task> tasks, byte expected) {
        for (Iterator<Task> iterator = tasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            if (box == null || !box.contains(task.pos) || results[index(task.pos)] != expected) {
                iterator.remove();
                refund(task);
            }
        }
    }

    private int index(BlockPos pos) {
        BlockPos rel = pos.subtract(box.min());
        return template.posToIndex(rel.getX(), rel.getY(), rel.getZ());
    }

    private static boolean isQueued(List<Task> tasks, BlockPos pos) {
        for (Task task : tasks) {
            if (task.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private void doTasks() {
        MjBattery battery = tile.getBattery();
        // Less power is used when the battery is low
        long max = Math.min((long) (MAX_POWER_PER_TICK * (double) (battery.getStored() + MAX_POWER_PER_TICK / 10)
            / (battery.getCapacity() * 2)), MAX_POWER_PER_TICK);
        ServerLevel level = (ServerLevel) tile.getLevel();
        for (Iterator<Task> iterator = breakTasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            task.power += battery.extractPower(0, Math.min(task.target - task.power, max / breakTasks.size()));
            if (task.power >= task.target) {
                level.destroyBlockProgress(task.pos.hashCode(), task.pos, -1);
                // Like in 1.12, what the filler breaks is lost
                if (BlockUtil.breakBlockAndGetDrops(level, task.pos, tile.getOwner()).isEmpty()) {
                    refund(task);
                }
                check(index(task.pos));
                iterator.remove();
            } else {
                level.destroyBlockProgress(task.pos.hashCode(), task.pos, (int) (task.power * 9 / task.target));
            }
        }
        for (Iterator<Task> iterator = placeTasks.iterator(); iterator.hasNext();) {
            Task task = iterator.next();
            task.power += battery.extractPower(0, Math.min(task.target - task.power, max / placeTasks.size()));
            if (task.power >= task.target) {
                if (!place(level, task)) {
                    tile.returnItem(task.item);
                }
                check(index(task.pos));
                iterator.remove();
            }
        }
    }

    /** Places the block of the task, like the owner of the filler would. */
    private boolean place(ServerLevel level, Task task) {
        if (!(task.item.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        FakePlayer player = FakePlayerFactory.get(level, tile.getOwner());
        ItemStack stack = task.item.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(task.pos), Direction.UP, task.pos, false);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        boolean placed = blockItem.place(context).consumesAction();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return placed;
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
            items.add(task.item);
        }
        return items;
    }

    @Override
    public String toString() {
        return "FillerBuilder[box=" + box + ", breaks=" + breakTasks.size() + ", places=" + placeTasks.size()
            + ", results=" + (results == null ? null : Arrays.toString(Arrays.copyOf(results, 0))) + "]";
    }
}
