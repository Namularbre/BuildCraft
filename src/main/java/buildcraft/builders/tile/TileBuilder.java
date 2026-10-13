/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.TilesAPI;

import buildcraft.lib.misc.data.Box;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockBuilder;
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.builders.item.ItemSnapshot;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.BlueprintTarget;
import buildcraft.builders.snapshot.BuildTarget;
import buildcraft.builders.snapshot.ISnapshotBuilderTile;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.SnapshotBuilder;
import buildcraft.builders.snapshot.SnapshotStorage;
import buildcraft.builders.snapshot.SnapshotTemplate;
import buildcraft.builders.snapshot.TemplateTarget;

/** Builds the template or blueprint in its slot, behind itself, with the items of its inventory. */
public class TileBuilder extends TileBC implements ITickable, ISnapshotBuilderTile {
    public static final IdAllocator IDS = TileBC.IDS.makeChild("builder");
    public static final int NET_TASKS = IDS.allocId("TASKS");
    /** How many required items are shown in the GUI. */
    public static final int DISPLAYED_ITEMS = 24;

    public final ItemStackHandler invSnapshot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return ItemSnapshot.getHeader(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            targetDirty = true;
            setChanged();
        }
    };
    public final ItemStackHandler invResources = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final MjBattery battery = new MjBattery(16_000 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));
    private final SnapshotBuilder builder = new SnapshotBuilder(this);
    private boolean canExcavate = true;
    private boolean targetDirty = true;
    private boolean hadTasks;
    /** The box being built (also on the client, for rendering). */
    @Nullable
    private Box box;
    /** The items still needed (only up to date on the client while the GUI is open). */
    public final List<ItemStack> clientRequired = new ArrayList<>();
    public boolean clientFinished;
    @Nullable
    public Snapshot.Type clientType;

    public TileBuilder(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.BUILDER_TILE.get(), pos, state);
        caps.addProvider(mjCaps);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, new CombinedInvWrapper(invSnapshot, invResources),
            EnumPipePart.VALUES);
        caps.addCapabilityInstance(TilesAPI.CAP_HAS_WORK, (IHasWork) () -> !builder.isFinished(), EnumPipePart.VALUES);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    public SnapshotBuilder getBuilder() {
        return builder;
    }

    @Nullable
    public Box getBox() {
        return box;
    }

    @Override
    public MjBattery getBattery() {
        return battery;
    }

    @Override
    public boolean canExcavate() {
        return canExcavate;
    }

    public void setCanExcavate(boolean canExcavate) {
        this.canExcavate = canExcavate;
        setChanged();
    }

    @Override
    public IItemHandler getResources() {
        return invResources;
    }

    @Override
    public void returnItem(ItemStack stack) {
        ItemStack leftOver = ItemHandlerHelper.insertItemStacked(invResources, stack, false);
        if (!leftOver.isEmpty() && level != null) {
            Block.popResource(level, worldPosition.above(), leftOver);
        }
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        List<ItemStack> drops = new ArrayList<>(builder.getTaskItems());
        drops.add(invSnapshot.getStackInSlot(0));
        for (int slot = 0; slot < invResources.getSlots(); slot++) {
            drops.add(invResources.getStackInSlot(slot));
        }
        return drops;
    }

    /** Works out what to build, from the snapshot in the slot. */
    private void updateTarget() {
        targetDirty = false;
        BuildTarget target = null;
        ItemSnapshot.Header header = ItemSnapshot.getHeader(invSnapshot.getStackInSlot(0));
        if (header != null && level instanceof ServerLevel serverLevel) {
            Snapshot snapshot = SnapshotStorage.get(serverLevel.getServer()).get(header.key());
            if (snapshot != null) {
                Direction facing = getBlockState().getValue(BlockBuilder.FACING);
                Rotation rotation = Rotation.NONE;
                for (Rotation r : Rotation.values()) {
                    if (r.rotate(snapshot.facing) == facing) {
                        rotation = r;
                    }
                }
                BlockPos origin = worldPosition.relative(facing.getOpposite()).offset(snapshot.offset.rotate(rotation));
                if (snapshot instanceof SnapshotTemplate template) {
                    target = new TemplateTarget(template.data, template.size, origin, rotation);
                } else if (snapshot instanceof Blueprint blueprint) {
                    target = new BlueprintTarget(blueprint, origin, rotation);
                }
            }
        }
        builder.setTarget(target);
        box = target == null ? null : target.box;
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }
        battery.tick(level, worldPosition);
        if (targetDirty) {
            updateTarget();
        }
        builder.tick();
        boolean hasTasks = !builder.breakTasks.isEmpty() || !builder.placeTasks.isEmpty();
        if (hasTasks || hadTasks) {
            createAndSendMessage(NET_TASKS, builder::writeTasks);
        }
        hadTasks = hasTasks;
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerBuilder(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("battery", battery.serializeNBT());
        nbt.put("snapshot", invSnapshot.serializeNBT());
        nbt.put("inv", invResources.serializeNBT());
        nbt.putBoolean("canExcavate", canExcavate);
        // Tasks aren't saved: give back the blocks that were on their way
        ListTag items = new ListTag();
        for (ItemStack item : builder.getTaskItems()) {
            items.add(item.save(new CompoundTag()));
        }
        nbt.put("taskItems", items);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        battery.deserializeNBT(nbt.getCompound("battery"));
        invSnapshot.deserializeNBT(nbt.getCompound("snapshot"));
        invResources.deserializeNBT(nbt.getCompound("inv"));
        canExcavate = !nbt.contains("canExcavate") || nbt.getBoolean("canExcavate");
        ListTag items = nbt.getList("taskItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemHandlerHelper.insertItemStacked(invResources, ItemStack.of(items.getCompound(i)), false);
        }
        targetDirty = true;
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                Box.write(buffer, box);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                buffer.writeBoolean(canExcavate);
                buffer.writeBoolean(builder.isFinished());
                BuildTarget target = builder.getTarget();
                buffer.writeByte(target instanceof BlueprintTarget ? 2 : target == null ? 0 : 1);
                List<ItemStack> required = builder.getRemainingRequiredItems();
                int count = Math.min(DISPLAYED_ITEMS, required.size());
                buffer.writeVarInt(count);
                for (int i = 0; i < count; i++) {
                    // The count can be more than a stack, so it is sent separately
                    buffer.writeItem(required.get(i).copyWithCount(1));
                    buffer.writeVarInt(required.get(i).getCount());
                }
                buffer.writeVarInt(builder.leftToBreak);
                buffer.writeVarInt(builder.leftToPlace);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                box = Box.read(buffer);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                canExcavate = buffer.readBoolean();
                clientFinished = buffer.readBoolean();
                int type = buffer.readByte();
                clientType = type == 0 ? null : type == 1 ? Snapshot.Type.TEMPLATE : Snapshot.Type.BLUEPRINT;
                clientRequired.clear();
                int count = buffer.readVarInt();
                for (int i = 0; i < count; i++) {
                    ItemStack stack = buffer.readItem();
                    stack.setCount(buffer.readVarInt());
                    clientRequired.add(stack);
                }
                builder.leftToBreak = buffer.readVarInt();
                builder.leftToPlace = buffer.readVarInt();
            } else if (id == NET_TASKS) {
                builder.readTasks(buffer);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        AABB bb = new AABB(worldPosition);
        return box == null ? bb : bb.minmax(box.toAABB());
    }
}
