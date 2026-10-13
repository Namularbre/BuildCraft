/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

import buildcraft.api.core.EnumPipePart;

import buildcraft.lib.misc.data.Box;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.container.ContainerArchitectTable;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotStorage;
import buildcraft.builders.snapshot.SnapshotTemplate;
import buildcraft.core.tile.TileMarkerVolume;

/** Scans the box of the markers it was placed against: a blank template or blueprint goes in, and comes out holding
 * the scan. */
public class TileArchitectTable extends TileBC implements ITickable {
    public static final IdAllocator IDS = TileBC.IDS.makeChild("architect");
    public static final int NET_SCAN = IDS.allocId("SCAN");

    /** A blank template or blueprint. */
    public final ItemStackHandler invIn = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof ItemSnapshot && ItemSnapshot.getHeader(stack) == null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public final ItemStackHandler invOut = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    @Nullable
    private Box box;
    public String name = "";
    /** How many blocks were scanned, or -1 if not scanning. */
    private int scanIndex = -1;
    private Snapshot.Type scanType;
    private BitSet templateData;
    private final List<BlockState> palette = new ArrayList<>();
    private int[] blueprintData;
    /** The block being scanned (on the client, for rendering). */
    @Nullable
    public BlockPos clientScanPos;
    public int clientProgress;

    public TileArchitectTable(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.ARCHITECT_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, new ExternalInventory(), EnumPipePart.VALUES);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    // Setting up the box

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level.isClientSide) {
            return;
        }
        Direction facing = getBlockState().getValue(BlockArchitectTable.FACING);
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof TileMarkerVolume marker) {
            Box markerBox = marker.getBox();
            if (markerBox != null) {
                marker.removeConnectedMarkers();
                setBox(markerBox);
            }
        }
    }

    public void setBox(@Nullable Box box) {
        this.box = box;
        scanIndex = -1;
        if (getBlockState().hasProperty(BlockArchitectTable.VALID)) {
            level.setBlock(worldPosition, getBlockState().setValue(BlockArchitectTable.VALID, box != null), 3);
        }
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    @Nullable
    public Box getBox() {
        return box;
    }

    /** @return How far the current scan is (from 0 to 1). */
    public double getProgress() {
        if (box == null || scanIndex < 0) {
            return 0;
        }
        return scanIndex / (double) getSize();
    }

    private int getSize() {
        BlockPos size = getBoxSize();
        return size.getX() * size.getY() * size.getZ();
    }

    private BlockPos getBoxSize() {
        return box.max().subtract(box.min()).offset(1, 1, 1);
    }

    private BlockPos posOf(int index) {
        BlockPos size = getBoxSize();
        int x = index % size.getX();
        int rest = index / size.getX();
        return box.min().offset(x, rest % size.getY(), rest / size.getY());
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        return List.of(invIn.getStackInSlot(0), invOut.getStackInSlot(0));
    }

    // Scanning

    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }
        ItemStack in = invIn.getStackInSlot(0);
        if (box == null || in.isEmpty() || !invOut.getStackInSlot(0).isEmpty()) {
            if (scanIndex >= 0) {
                scanIndex = -1;
                sendScan(null);
            }
            return;
        }
        if (scanIndex < 0) {
            scanType = ((ItemSnapshot) in.getItem()).type;
            scanIndex = 0;
            templateData = new BitSet(getSize());
            palette.clear();
            blueprintData = new int[getSize()];
        }
        int perTick = scanType == Snapshot.Type.TEMPLATE ? 900 : 300;
        BlockPos last = null;
        for (int i = 0; i < perTick && scanIndex < getSize(); i++) {
            last = posOf(scanIndex);
            BlockState state = level.getBlockState(last);
            if (scanType == Snapshot.Type.TEMPLATE) {
                templateData.set(scanIndex, !state.isAir() && !(state.getBlock() instanceof LiquidBlock));
            } else {
                BlockState scanned = Blueprint.toScanned(state);
                int index = palette.indexOf(scanned);
                if (index < 0) {
                    index = palette.size();
                    palette.add(scanned);
                }
                blueprintData[scanIndex] = index;
            }
            scanIndex++;
        }
        sendScan(last);
        if (scanIndex >= getSize()) {
            finishScanning();
        }
    }

    private void sendScan(@Nullable BlockPos pos) {
        createAndSendMessage(NET_SCAN, buffer -> {
            buffer.writeBoolean(pos != null);
            if (pos != null) {
                buffer.writeBlockPos(pos);
            }
            buffer.writeVarInt((int) (getProgress() * 1000));
        });
    }

    private void finishScanning() {
        Direction facing = getBlockState().getValue(BlockArchitectTable.FACING);
        Snapshot snapshot;
        if (scanType == Snapshot.Type.TEMPLATE) {
            SnapshotTemplate template = new SnapshotTemplate();
            template.data = templateData;
            snapshot = template;
        } else {
            Blueprint blueprint = new Blueprint();
            blueprint.palette.addAll(palette);
            blueprint.data = blueprintData;
            snapshot = blueprint;
        }
        snapshot.size = getBoxSize();
        snapshot.facing = facing;
        snapshot.offset = box.min().subtract(worldPosition.relative(facing.getOpposite()));
        String key = SnapshotStorage.get(((ServerLevel) level).getServer()).add(snapshot);

        ItemStack in = invIn.extractItem(0, 1, false);
        ItemSnapshot item = (ItemSnapshot) in.getItem();
        String ownerName = getOwner().getName() == null ? "" : getOwner().getName();
        UUID ownerId = getOwner().getId() == null ? new UUID(0, 0) : getOwner().getId();
        invOut.setStackInSlot(0, item.createUsed(
            new ItemSnapshot.Header(key, name, ownerId, ownerName, System.currentTimeMillis())
        ));
        scanIndex = -1;
        templateData = null;
        blueprintData = null;
        palette.clear();
        sendScan(null);
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (box == null) {
                player.displayClientMessage(Component.translatable("buildcraft.architect.no_box"), true);
            } else {
                openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                    (id, inventory, p) -> new ContainerArchitectTable(id, p, this));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("in", invIn.serializeNBT());
        nbt.put("out", invOut.serializeNBT());
        if (box != null) {
            nbt.put("box", box.save());
        }
        nbt.putString("name", name);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        invIn.deserializeNBT(nbt.getCompound("in"));
        invOut.deserializeNBT(nbt.getCompound("out"));
        box = nbt.contains("box") ? Box.load(nbt.getCompound("box")) : null;
        name = nbt.getString("name");
        // An unfinished scan starts again
        scanIndex = -1;
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            Box.write(buffer, box);
            buffer.writeUtf(name);
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                box = Box.read(buffer);
                name = buffer.readUtf();
            } else if (id == NET_SCAN) {
                clientScanPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
                clientProgress = buffer.readVarInt();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB bb = new AABB(worldPosition);
        return box == null ? bb : bb.minmax(box.toAABB());
    }

    /** What pipes see: blank snapshots go in (slot 0), scanned ones come out (slot 1). */
    private class ExternalInventory implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? invIn.getStackInSlot(0) : invOut.getStackInSlot(0);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == 0 ? invIn.insertItem(0, stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? invOut.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && invIn.isItemValid(0, stack);
        }
    }
}
