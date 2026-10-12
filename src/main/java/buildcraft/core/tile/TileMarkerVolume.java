/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.lib.misc.data.Box;
import buildcraft.lib.tile.TileBC;

import buildcraft.core.BCCoreBlocks;

/** A marker that connects to other markers in line with it (up to 64 blocks away) when clicked, to mark out a box
 * (for example the area of a quarry). A redstone signal makes it show the lines along which it looks. */
public class TileMarkerVolume extends TileBC {
    public static final int MAX_DISTANCE = 64;

    /** The markers of the box this marker is part of (itself included), or nothing if it isn't connected. */
    private final List<BlockPos> connection = new ArrayList<>();
    private boolean showSignals = false;

    public TileMarkerVolume(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.MARKER_VOLUME_TILE.get(), pos, state);
    }

    public List<BlockPos> getConnection() {
        return connection;
    }

    public boolean isShowingSignals() {
        return showSignals;
    }

    /** @return The box between the connected markers, or null if this marker isn't connected. */
    @Nullable
    public Box getBox() {
        return connection.size() < 2 ? null : boxOf(new LinkedHashSet<>(connection));
    }

    /** @return The axes along which this marker is connected. */
    public Set<Direction.Axis> getConnectedAxes() {
        Set<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
        for (BlockPos other : connection) {
            BlockPos diff = other.subtract(worldPosition);
            int nonZero = (diff.getX() != 0 ? 1 : 0) + (diff.getY() != 0 ? 1 : 0) + (diff.getZ() != 0 ? 1 : 0);
            if (nonZero == 1) {
                axes.add(diff.getX() != 0 ? Direction.Axis.X : diff.getY() != 0 ? Direction.Axis.Y : Direction.Axis.Z);
            }
        }
        return axes;
    }

    @Nullable
    private TileMarkerVolume getMarker(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileMarkerVolume marker ? marker : null;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            tryConnect();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Connects to the nearest marker in each direction (on each axis that isn't connected yet), and to the markers
     * on the other corners of the box. */
    public void tryConnect() {
        Set<BlockPos> markers = new LinkedHashSet<>(connection.isEmpty() ? List.of(worldPosition) : connection);
        Set<Direction.Axis> taken = getConnectedAxes();
        for (Direction face : Direction.values()) {
            if (taken.contains(face.getAxis())) {
                continue;
            }
            for (int i = 1; i <= MAX_DISTANCE; i++) {
                BlockPos toTry = worldPosition.relative(face, i);
                if (getMarker(toTry) != null) {
                    markers.add(toTry);
                    taken.add(face.getAxis());
                    break;
                }
            }
        }
        if (markers.size() < 2) {
            return;
        }
        // Markers on the other corners of the box join it too
        Box box = boxOf(markers);
        for (int x : new int[] { box.min().getX(), box.max().getX() }) {
            for (int y : new int[] { box.min().getY(), box.max().getY() }) {
                for (int z : new int[] { box.min().getZ(), box.max().getZ() }) {
                    BlockPos corner = new BlockPos(x, y, z);
                    if (getMarker(corner) != null) {
                        markers.add(corner);
                    }
                }
            }
        }
        for (BlockPos pos : markers) {
            TileMarkerVolume marker = getMarker(pos);
            if (marker != null && !markers.containsAll(marker.connection)) {
                marker.leaveConnection();
            }
        }
        for (BlockPos pos : markers) {
            TileMarkerVolume marker = getMarker(pos);
            if (marker != null) {
                marker.setConnection(new ArrayList<>(markers));
            }
        }
    }

    private static Box boxOf(Set<BlockPos> positions) {
        BlockPos first = positions.iterator().next();
        BlockPos min = first, max = first;
        for (BlockPos pos : positions) {
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()),
                Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()),
                Math.max(max.getZ(), pos.getZ()));
        }
        return new Box(min, max);
    }

    private void setConnection(List<BlockPos> markers) {
        connection.clear();
        connection.addAll(markers);
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    /** Takes this marker out of its box. The other markers stay connected, if there are at least 2 of them. */
    public void leaveConnection() {
        if (connection.isEmpty()) {
            return;
        }
        List<BlockPos> others = new ArrayList<>(connection);
        others.remove(worldPosition);
        setConnection(List.of());
        for (BlockPos pos : others) {
            TileMarkerVolume marker = getMarker(pos);
            if (marker != null) {
                List<BlockPos> remaining = new ArrayList<>(others);
                marker.setConnection(remaining.size() >= 2 ? remaining : List.of());
            }
        }
    }

    /** Removes every marker of the box from the world (dropping them), once something (like a quarry) took its
     * area. */
    public void removeConnectedMarkers() {
        List<BlockPos> markers = new ArrayList<>(connection.isEmpty() ? List.of(worldPosition) : connection);
        for (BlockPos pos : markers) {
            TileMarkerVolume marker = getMarker(pos);
            if (marker != null) {
                marker.connection.clear();
                level.destroyBlock(pos, true);
            }
        }
    }

    // Redstone shows the lines along which the marker looks for others

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        updateSignals();
    }

    @Override
    public void onNeighbourBlockChanged(Block block, BlockPos neighbour) {
        updateSignals();
    }

    private void updateSignals() {
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered != showSignals && !level.isClientSide) {
            showSignals = powered;
            setChanged();
            sendNetworkUpdate(NET_RENDER_DATA);
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (!level.isClientSide) {
            leaveConnection();
        }
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ListTag list = new ListTag();
        for (BlockPos pos : connection) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        nbt.put("connection", list);
        nbt.putBoolean("showSignals", showSignals);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        connection.clear();
        ListTag list = nbt.getList("connection", Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            connection.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
        }
        showSignals = nbt.getBoolean("showSignals");
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            buffer.writeBoolean(showSignals);
            buffer.writeVarInt(connection.size());
            connection.forEach(buffer::writeBlockPos);
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_RENDER_DATA) {
            showSignals = buffer.readBoolean();
            connection.clear();
            int count = buffer.readVarInt();
            for (int i = 0; i < count; i++) {
                connection.add(buffer.readBlockPos());
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        // The lines go far from the marker
        return new AABB(worldPosition).inflate(MAX_DISTANCE);
    }
}
