/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.wrapper.EmptyHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;

import buildcraft.lib.inventory.InventoryUtil;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.data.Box;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.factory.BCFactoryConfig;
import buildcraft.factory.tile.TileMiningWell;

/** Builds a frame around an area, then mines everything below it with a drill, layer by layer, sending what it mines
 * to the pipes and inventories next to it. Keeps the chunks of its area loaded. */
public class TileQuarry extends TileBC implements ITickable {
    public static final long MAX_POWER_PER_TICK = 512 * MjAPI.MJ;
    /** The power needed to place one frame block. */
    public static final long FRAME_POWER = 24 * MjAPI.MJ;
    /** The power needed to move the drill one block. */
    public static final long MOVE_POWER_PER_BLOCK = 20 * MjAPI.MJ;
    /** Ticks between two looks for blocks to mine, once the area seemed done (blocks can fall or flow in). */
    private static final int RESCAN_DELAY = 100;

    private final MjBattery battery = new MjBattery(24_000 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));

    /** The frame: the edges of this box. */
    @Nullable
    private Box frameBox;
    /** Everything below the frame, down to the maximum mining depth. */
    @Nullable
    private Box miningBox;
    /** Where frame blocks go, in the order they are placed (spreading out from the quarry). */
    public final List<BlockPos> framePoses = new ArrayList<>();

    // Checking the frame area: everything in it must be cleared, and frame blocks placed on its edges
    private final Deque<BlockPos> toCheck = new ArrayDeque<>();
    private final Set<BlockPos> firstCheckedPoses = new HashSet<>();
    private boolean firstChecked = false;
    private final Set<BlockPos> frameBreakBlockPoses =
        new TreeSet<>(Comparator.comparingDouble((BlockPos p) -> p.distSqr(worldPosition)).thenComparing(p -> p));
    private final Set<BlockPos> framePlaceFramePoses = new HashSet<>();

    @Nullable
    private MiningIterator iterator;
    private long nextRescan;
    @Nullable
    public Task currentTask;
    /** Where the drill is (the lowest corner of the block it is above), or null while the frame is being built. */
    @Nullable
    private Vec3 drillPos;
    // Client side: the drill position of the last tick, to move smoothly
    @Nullable
    private Vec3 prevClientDrillPos;
    @Nullable
    private Vec3 clientDrillPos;

    private boolean chunksLoaded = false;

    public TileQuarry(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.QUARRY_TILE.get(), pos, state);
        caps.addProvider(mjCaps);
        // Nothing can be put in or taken out, but item pipes connect to it (to get what it mines)
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, EmptyHandler.INSTANCE, EnumPipePart.VALUES);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    @Nullable
    public Box getFrameBox() {
        return frameBox;
    }

    @Nullable
    public Box getMiningBox() {
        return miningBox;
    }

    // Setting up the area

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level.isClientSide) {
            return;
        }
        // The area is behind the quarry: 11 by 11 blocks (the frame included)
        BlockPos min, max;
        switch (getFacing().getOpposite()) {
            case WEST -> {
                min = worldPosition.offset(-11, 0, -5);
                max = worldPosition.offset(-1, 4, 5);
            }
            case SOUTH -> {
                min = worldPosition.offset(-5, 0, 1);
                max = worldPosition.offset(5, 4, 11);
            }
            case NORTH -> {
                min = worldPosition.offset(-5, 0, -11);
                max = worldPosition.offset(5, 4, -1);
            }
            default -> {
                min = worldPosition.offset(1, 0, -5);
                max = worldPosition.offset(11, 4, 5);
            }
        }
        setArea(min, max);
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockQuarry.FACING) ? state.getValue(BlockQuarry.FACING) : Direction.NORTH;
    }

    /** Sets the frame to the edges of the given box (made taller if needed), and mines below it. */
    public void setArea(BlockPos min, BlockPos max) {
        int minHeight = BCBuildersConfig.quarryFrameMinHeight();
        if (max.getY() - min.getY() < minHeight) {
            max = new BlockPos(max.getX(), min.getY() + minHeight, max.getZ());
        }
        if (max.getY() >= level.getMaxBuildHeight()) {
            int dist = max.getY() - level.getMaxBuildHeight() + 1;
            min = min.below(dist);
            max = max.below(dist);
        }
        releaseChunks();
        frameBox = new Box(min, max);
        int minY = Math.max(level.getMinBuildHeight(), max.getY() - 1 - BCFactoryConfig.miningMaxDepth());
        miningBox = new Box(
            new BlockPos(min.getX() + 1, minY, min.getZ() + 1),
            new BlockPos(max.getX() - 1, max.getY() - 1, max.getZ() - 1)
        );
        iterator = null;
        drillPos = null;
        currentTask = null;
        updatePoses();
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    private void updatePoses() {
        framePoses.clear();
        toCheck.clear();
        firstCheckedPoses.clear();
        firstChecked = false;
        frameBreakBlockPoses.clear();
        framePlaceFramePoses.clear();
        if (frameBox == null) {
            return;
        }
        List<BlockPos> blocksInArea = frameBox.getBlocksInArea();
        blocksInArea.sort(Comparator.comparingDouble(p -> p.distSqr(worldPosition)));
        toCheck.addAll(blocksInArea);
        framePoses.addAll(getFramePositions());
        loadChunks();
    }

    /** @return The positions of the frame, spreading out from the quarry along the edges of the frame box. */
    private List<BlockPos> getFramePositions() {
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> framePositions = new ArrayList<>();
        List<BlockPos> openSet = new ArrayList<>(List.of(worldPosition));
        List<Direction> order = new ArrayList<>(List.of(Direction.values()));
        while (!openSet.isEmpty()) {
            List<BlockPos> nextOpenSet = new ArrayList<>();
            for (BlockPos p : openSet) {
                Collections.shuffle(order);
                for (Direction face : order) {
                    BlockPos next = p.relative(face);
                    if (frameBox.isOnEdge(next) && visited.add(next)) {
                        nextOpenSet.add(next);
                        framePositions.add(next);
                    }
                }
            }
            Collections.shuffle(nextOpenSet);
            openSet = nextOpenSet;
        }
        return framePositions;
    }

    // What can be done where

    /** @return True if the drill can break the block (or there is nothing there). */
    private boolean canMine(BlockPos pos) {
        return level.getBlockState(pos).isAir() || BlockUtil.canMachineBreak(level, pos);
    }

    /** @return True if the drill can go through the block: air, or a fluid no thicker than water. */
    private boolean canMoveThrough(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        FluidState fluid = state.getFluidState();
        return state.getBlock() instanceof LiquidBlock && fluid.getFluidType().getViscosity() <= 1000;
    }

    /** @return True if everything above the given position (in the mining area) has been mined. */
    private boolean canMoveDownTo(BlockPos pos) {
        for (int y = miningBox.max().getY(); y > pos.getY(); y--) {
            if (!canMoveThrough(new BlockPos(pos.getX(), y, pos.getZ()))) {
                return false;
            }
        }
        return true;
    }

    /** @return True if there is a solid block (not a fluid) at the given position. */
    private boolean isSolid(BlockPos pos) {
        return !level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty();
    }

    /** Works out what to do at the given position of the frame box: break what is in the way, or place frame. */
    private void check(BlockPos pos) {
        frameBreakBlockPoses.remove(pos);
        framePlaceFramePoses.remove(pos);
        if (frameBox.isOnEdge(pos)) {
            if (!level.getBlockState(pos).is(BCBuildersBlocks.FRAME.get())) {
                if (isSolid(pos)) {
                    frameBreakBlockPoses.add(pos);
                } else {
                    framePlaceFramePoses.add(pos);
                }
            }
        } else if (isSolid(pos)) {
            frameBreakBlockPoses.add(pos);
        }
        if (!firstChecked) {
            firstCheckedPoses.add(pos);
            if (firstCheckedPoses.size() >= toCheck.size()) {
                firstChecked = true;
                firstCheckedPoses.clear();
            }
        }
    }

    // Working

    @Override
    public void tick() {
        if (level.isClientSide) {
            prevClientDrillPos = clientDrillPos;
            clientDrillPos = drillPos;
            if (currentTask != null) {
                currentTask.clientTick();
            }
            return;
        }
        if (frameBox == null || miningBox == null) {
            return;
        }
        if (!chunksLoaded) {
            loadChunks();
        }

        // Keep checking the frame area, quickly until everything has been checked once
        for (int i = 0; i < (firstChecked ? 10 : 500) && !toCheck.isEmpty(); i++) {
            BlockPos pos = toCheck.pollFirst();
            check(pos);
            toCheck.addLast(pos);
        }
        if (!firstChecked) {
            return;
        }

        // Less power is used when the battery is low
        long max = MAX_POWER_PER_TICK;
        if (battery.getStored() <= battery.getCapacity() / 2) {
            double ratio = (battery.getStored() + MjAPI.MJ / 2) / (double) (battery.getCapacity() / 2);
            max = (long) Math.min(MAX_POWER_PER_TICK, MAX_POWER_PER_TICK * ratio);
        }
        int maxTasks = (int) Math.max(1, max * BCBuildersConfig.quarryMaxTasksPerTick() / MAX_POWER_PER_TICK);
        boolean sendUpdate = false;

        for (int i = 0; i < maxTasks; i++) {
            if (currentTask != null) {
                long added = battery.extractPower(0, Math.min(max, currentTask.getRequiredPowerThisTick()));
                max -= added;
                if (currentTask.addPower(added)) {
                    currentTask = null;
                    sendUpdate = true;
                } else {
                    sendUpdate = true;
                    break;
                }
            }
            if (!chooseTask()) {
                break;
            }
            sendUpdate = true;
        }
        if (sendUpdate) {
            sendNetworkUpdate(NET_RENDER_DATA);
            setChanged();
        }
    }

    /** Picks the next thing to do: clear the frame area, then build the frame, then mine.
     *
     * @return True if there is something to do. */
    private boolean chooseTask() {
        if (!frameBreakBlockPoses.isEmpty()) {
            BlockPos pos = frameBreakBlockPoses.iterator().next();
            check(pos);
            if (canMine(pos) && isSolid(pos)) {
                drillPos = null;
                currentTask = new TaskBreakBlock(pos);
            }
            return true;
        }
        for (BlockPos pos : framePoses) {
            if (framePlaceFramePoses.contains(pos)) {
                check(pos);
                if (framePlaceFramePoses.contains(pos)) {
                    drillPos = null;
                    currentTask = new TaskAddFrame(pos);
                    return true;
                }
            }
        }

        if (iterator == null || drillPos == null) {
            iterator = new MiningIterator(miningBox, worldPosition);
            drillPos = Vec3.atLowerCornerOf(miningBox.closestInsideTo(worldPosition));
        }
        BlockPos target = findNextBlock();
        if (target == null) {
            // Everything seems mined: look again later, in case blocks fell or flowed in
            if (level.getGameTime() >= nextRescan) {
                nextRescan = level.getGameTime() + RESCAN_DELAY;
                iterator = new MiningIterator(miningBox, worldPosition);
            }
            return false;
        }
        Vec3 targetVec = Vec3.atLowerCornerOf(target);
        if (drillPos.distanceToSqr(targetVec) >= 1) {
            currentTask = new TaskMoveDrill(drillPos, targetVec);
        } else {
            currentTask = new TaskBreakBlock(target);
        }
        return true;
    }

    /** @return The next block to mine (that the drill can reach), or null if there are none left. */
    @Nullable
    private BlockPos findNextBlock() {
        while (iterator.getCurrent() != null) {
            BlockPos current = iterator.getCurrent();
            if (!canMoveThrough(current) && canMine(current) && canMoveDownTo(current)) {
                return current;
            }
            iterator.advance();
        }
        return null;
    }

    // Chunk loading

    private void loadChunks() {
        if (!(level instanceof ServerLevel serverLevel) || frameBox == null) {
            return;
        }
        forceChunks(serverLevel, true);
        chunksLoaded = true;
    }

    private void releaseChunks() {
        if (level instanceof ServerLevel serverLevel && frameBox != null && chunksLoaded) {
            forceChunks(serverLevel, false);
        }
        chunksLoaded = false;
    }

    private void forceChunks(ServerLevel serverLevel, boolean add) {
        ChunkPos minChunk = new ChunkPos(frameBox.min());
        ChunkPos maxChunk = new ChunkPos(frameBox.max());
        for (int x = minChunk.x; x <= maxChunk.x; x++) {
            for (int z = minChunk.z; z <= maxChunk.z; z++) {
                ForgeChunkManager.forceChunk(serverLevel, BuildCraft.MODID, worldPosition, x, z, add, true);
            }
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        releaseChunks();
        // The frame goes with the quarry
        for (BlockPos pos : framePoses) {
            if (level.getBlockState(pos).is(BCBuildersBlocks.FRAME.get())) {
                level.removeBlock(pos, false);
            }
        }
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (frameBox != null && miningBox != null) {
            nbt.put("frame", frameBox.save());
            nbt.put("box", miningBox.save());
        }
        if (iterator != null) {
            nbt.put("iterator", iterator.save());
        }
        nbt.put("battery", battery.serializeNBT());
        if (currentTask != null) {
            nbt.putInt("currentTaskId", currentTask.getType());
            nbt.put("currentTaskData", currentTask.save());
        }
        if (drillPos != null) {
            nbt.putDouble("drillX", drillPos.x);
            nbt.putDouble("drillY", drillPos.y);
            nbt.putDouble("drillZ", drillPos.z);
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        frameBox = Box.load(nbt.getCompound("frame"));
        miningBox = Box.load(nbt.getCompound("box"));
        iterator = miningBox != null && nbt.contains("iterator")
            ? MiningIterator.load(miningBox, nbt.getCompound("iterator")) : null;
        battery.deserializeNBT(nbt.getCompound("battery"));
        currentTask = nbt.contains("currentTaskId") ? createTask(nbt.getInt("currentTaskId")) : null;
        if (currentTask != null) {
            currentTask.load(nbt.getCompound("currentTaskData"));
        }
        drillPos = nbt.contains("drillX")
            ? new Vec3(nbt.getDouble("drillX"), nbt.getDouble("drillY"), nbt.getDouble("drillZ")) : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide) {
            updatePoses();
        }
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            Box.write(buffer, frameBox);
            Box.write(buffer, miningBox);
            buffer.writeBoolean(drillPos != null);
            if (drillPos != null) {
                buffer.writeDouble(drillPos.x);
                buffer.writeDouble(drillPos.y);
                buffer.writeDouble(drillPos.z);
            }
            buffer.writeBoolean(currentTask != null);
            if (currentTask != null) {
                buffer.writeByte(currentTask.getType());
                currentTask.write(buffer);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_RENDER_DATA) {
            frameBox = Box.read(buffer);
            miningBox = Box.read(buffer);
            drillPos = buffer.readBoolean() ? new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
                : null;
            if (buffer.readBoolean()) {
                Task task = createTask(buffer.readByte());
                task.read(buffer);
                if (currentTask != null && currentTask.isSameAs(task)) {
                    // Keeps the client power of the last tick, for smooth rendering
                    currentTask.power = task.power;
                } else {
                    currentTask = task;
                }
            } else {
                currentTask = null;
            }
        }
    }

    // Rendering

    /** @return Where the drill is, smoothly moving between ticks, or null while the frame is being built. */
    @Nullable
    public Vec3 getDrillPos(float partialTicks) {
        if (clientDrillPos == null || prevClientDrillPos == null) {
            return null;
        }
        return prevClientDrillPos.add(clientDrillPos.subtract(prevClientDrillPos).scale(partialTicks));
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB box = new AABB(worldPosition);
        if (frameBox != null) {
            box = box.minmax(frameBox.toAABB());
        }
        if (drillPos != null) {
            box = box.minmax(new AABB(drillPos, drillPos.add(1, 1, 1)));
        }
        return box;
    }

    // Tasks

    private Task createTask(int type) {
        return switch (type) {
            case 0 -> new TaskBreakBlock(BlockPos.ZERO);
            case 1 -> new TaskAddFrame(BlockPos.ZERO);
            default -> new TaskMoveDrill(Vec3.ZERO, Vec3.ZERO);
        };
    }

    /** Something that the quarry does with power: breaking a block, placing frame, or moving the drill. */
    public abstract class Task {
        public long power;
        public long clientPower, prevClientPower;

        abstract int getType();

        public abstract long getTarget();

        long getRequiredPowerThisTick() {
            return Math.max(0, getTarget() - power);
        }

        /** @return True if the task is done (or can't be done anymore). */
        final boolean addPower(long microJoules) {
            power += microJoules;
            if (power >= getTarget()) {
                if (!finish()) {
                    // Nothing was done: give the power back
                    battery.addPowerChecking(power, false);
                }
                return true;
            }
            return onReceivePower();
        }

        /** @return True if the task can't be done anymore. */
        protected abstract boolean onReceivePower();

        /** @return True if the task was done. */
        protected abstract boolean finish();

        abstract boolean isSameAs(Task other);

        void clientTick() {
            prevClientPower = clientPower;
            clientPower = power;
        }

        CompoundTag save() {
            CompoundTag nbt = new CompoundTag();
            nbt.putLong("power", power);
            return nbt;
        }

        void load(CompoundTag nbt) {
            power = Math.max(0, nbt.getLong("power"));
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeLong(power);
        }

        void read(FriendlyByteBuf buffer) {
            power = buffer.readLong();
        }
    }

    public class TaskBreakBlock extends Task {
        public BlockPos breakPos;

        TaskBreakBlock(BlockPos breakPos) {
            this.breakPos = breakPos;
        }

        @Override
        int getType() {
            return 0;
        }

        @Override
        public long getTarget() {
            return level instanceof ServerLevel serverLevel ? TileMiningWell.getBreakPower(serverLevel, breakPos)
                : Math.max(1, power);
        }

        @Override
        protected boolean onReceivePower() {
            if (level.getBlockState(breakPos).isAir()) {
                return true;
            }
            level.destroyBlockProgress(breakPos.hashCode(), breakPos, (int) (power * 9 / getTarget()));
            return false;
        }

        @Override
        protected boolean finish() {
            if (!canMine(breakPos)) {
                return true;
            }
            level.destroyBlockProgress(breakPos.hashCode(), breakPos, -1);
            boolean broken = BlockUtil.breakBlockAndGetDrops((ServerLevel) level, breakPos, getOwner())
                .map(drops -> {
                    // While clearing the frame area, what is broken is destroyed rather than kept
                    if (drillPos != null) {
                        drops.forEach(stack -> InventoryUtil.addToBestAcceptor(level, worldPosition, stack));
                    }
                    return true;
                }).orElse(false);
            check(breakPos);
            return broken;
        }

        @Override
        boolean isSameAs(Task other) {
            return other instanceof TaskBreakBlock task && task.breakPos.equals(breakPos);
        }

        @Override
        CompoundTag save() {
            CompoundTag nbt = super.save();
            nbt.putLong("pos", breakPos.asLong());
            return nbt;
        }

        @Override
        void load(CompoundTag nbt) {
            super.load(nbt);
            breakPos = BlockPos.of(nbt.getLong("pos"));
        }

        @Override
        void write(FriendlyByteBuf buffer) {
            super.write(buffer);
            buffer.writeBlockPos(breakPos);
            // The client can't work out the hardness of every block: send the target too
            buffer.writeLong(getTarget());
        }

        private long clientTarget = 1;

        @Override
        void read(FriendlyByteBuf buffer) {
            super.read(buffer);
            breakPos = buffer.readBlockPos();
            clientTarget = Math.max(1, buffer.readLong());
        }

        /** @return How far (from 0 to 1) the block is broken, on the client. */
        public double getClientProgress(float partialTicks) {
            return (prevClientPower + (clientPower - prevClientPower) * partialTicks) / clientTarget;
        }
    }

    public class TaskAddFrame extends Task {
        public BlockPos framePos;

        TaskAddFrame(BlockPos framePos) {
            this.framePos = framePos;
        }

        @Override
        int getType() {
            return 1;
        }

        @Override
        public long getTarget() {
            return FRAME_POWER;
        }

        @Override
        protected boolean onReceivePower() {
            return isSolid(framePos);
        }

        @Override
        protected boolean finish() {
            if (isSolid(framePos)) {
                return false;
            }
            level.setBlock(framePos, BCBuildersBlocks.FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            check(framePos);
            return true;
        }

        @Override
        boolean isSameAs(Task other) {
            return other instanceof TaskAddFrame task && task.framePos.equals(framePos);
        }

        @Override
        CompoundTag save() {
            CompoundTag nbt = super.save();
            nbt.putLong("pos", framePos.asLong());
            return nbt;
        }

        @Override
        void load(CompoundTag nbt) {
            super.load(nbt);
            framePos = BlockPos.of(nbt.getLong("pos"));
        }

        @Override
        void write(FriendlyByteBuf buffer) {
            super.write(buffer);
            buffer.writeBlockPos(framePos);
        }

        @Override
        void read(FriendlyByteBuf buffer) {
            super.read(buffer);
            framePos = buffer.readBlockPos();
        }
    }

    public class TaskMoveDrill extends Task {
        public Vec3 from, to;

        TaskMoveDrill(Vec3 from, Vec3 to) {
            this.from = from;
            this.to = to;
        }

        @Override
        int getType() {
            return 2;
        }

        @Override
        public long getTarget() {
            return Math.max(1, (long) (from.distanceTo(to) * MOVE_POWER_PER_BLOCK));
        }

        @Override
        long getRequiredPowerThisTick() {
            long required = super.getRequiredPowerThisTick();
            double maxSpeed = BCBuildersConfig.quarryMaxFrameMoveSpeed();
            if (maxSpeed < 0.1) {
                return required;
            }
            // Blocks per second -> power per tick
            return Math.min(required, (long) (maxSpeed / 20 * MOVE_POWER_PER_BLOCK));
        }

        @Override
        protected boolean onReceivePower() {
            double progress = power / (double) getTarget();
            drillPos = from.scale(1 - progress).add(to.scale(progress));
            return false;
        }

        @Override
        protected boolean finish() {
            drillPos = to;
            return true;
        }

        @Override
        boolean isSameAs(Task other) {
            return other instanceof TaskMoveDrill task && task.from.equals(from) && task.to.equals(to);
        }

        @Override
        CompoundTag save() {
            CompoundTag nbt = super.save();
            writeVec(nbt, "from", from);
            writeVec(nbt, "to", to);
            return nbt;
        }

        @Override
        void load(CompoundTag nbt) {
            super.load(nbt);
            from = readVec(nbt, "from");
            to = readVec(nbt, "to");
        }

        @Override
        void write(FriendlyByteBuf buffer) {
            super.write(buffer);
            for (Vec3 v : new Vec3[] { from, to }) {
                buffer.writeDouble(v.x);
                buffer.writeDouble(v.y);
                buffer.writeDouble(v.z);
            }
        }

        @Override
        void read(FriendlyByteBuf buffer) {
            super.read(buffer);
            from = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            to = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        private static void writeVec(CompoundTag nbt, String key, Vec3 v) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("x", v.x);
            tag.putDouble("y", v.y);
            tag.putDouble("z", v.z);
            nbt.put(key, tag);
        }

        private static Vec3 readVec(CompoundTag nbt, String key) {
            CompoundTag tag = nbt.getCompound(key);
            return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
        }
    }

    /** Goes through the mining area from the top layer down, zigzagging across each layer. */
    static final class MiningIterator {
        private final Box box;
        /** True to go along X first (then Z), false for the other way round. */
        private final boolean xFirst;
        private int a, b, y;
        private boolean aForward, bForward;
        private boolean done;

        MiningIterator(Box box, BlockPos quarryPos) {
            // Like 1.12, the order depends on where the quarry is
            this(box, (quarryPos.asLong() & 1) == 0, (quarryPos.asLong() & 2) == 0, (quarryPos.asLong() & 4) == 0);
        }

        private MiningIterator(Box box, boolean xFirst, boolean aForward, boolean bForward) {
            this.box = box;
            this.xFirst = xFirst;
            this.aForward = aForward;
            this.bForward = bForward;
            this.a = aForward ? aMin() : aMax();
            this.b = bForward ? bMin() : bMax();
            this.y = box.max().getY();
        }

        private int aMin() {
            return xFirst ? box.min().getX() : box.min().getZ();
        }

        private int aMax() {
            return xFirst ? box.max().getX() : box.max().getZ();
        }

        private int bMin() {
            return xFirst ? box.min().getZ() : box.min().getX();
        }

        private int bMax() {
            return xFirst ? box.max().getZ() : box.max().getX();
        }

        @Nullable
        BlockPos getCurrent() {
            if (done) {
                return null;
            }
            return xFirst ? new BlockPos(a, y, b) : new BlockPos(b, y, a);
        }

        void advance() {
            a += aForward ? 1 : -1;
            if (a < aMin() || a > aMax()) {
                aForward = !aForward;
                a = aForward ? aMin() : aMax();
                b += bForward ? 1 : -1;
                if (b < bMin() || b > bMax()) {
                    bForward = !bForward;
                    b = bForward ? bMin() : bMax();
                    y--;
                    if (y < box.min().getY()) {
                        done = true;
                    }
                }
            }
        }

        CompoundTag save() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("xFirst", xFirst);
            nbt.putInt("a", a);
            nbt.putInt("b", b);
            nbt.putInt("y", y);
            nbt.putBoolean("aForward", aForward);
            nbt.putBoolean("bForward", bForward);
            nbt.putBoolean("done", done);
            return nbt;
        }

        static MiningIterator load(Box box, CompoundTag nbt) {
            MiningIterator iterator = new MiningIterator(
                box, nbt.getBoolean("xFirst"), nbt.getBoolean("aForward"), nbt.getBoolean("bForward")
            );
            iterator.a = nbt.getInt("a");
            iterator.b = nbt.getInt("b");
            iterator.y = nbt.getInt("y");
            iterator.done = nbt.getBoolean("done");
            return iterator;
        }
    }
}
