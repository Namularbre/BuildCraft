/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.TilesAPI;

import buildcraft.lib.inventory.InsertOnlyItemHandler;
import buildcraft.lib.misc.data.Box;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.filler.FillerType;
import buildcraft.builders.filler.Template;
import buildcraft.builders.snapshot.ISnapshotBuilderTile;
import buildcraft.builders.snapshot.SnapshotBuilder;
import buildcraft.builders.snapshot.TemplateTarget;
import buildcraft.core.tile.TileMarkerVolume;

/** Fills (or clears) the box of the markers it was placed against with a pattern, using the blocks in its inventory. */
public class TileFiller extends TileBC
    implements ITickable, IFillerStatementContainer, IControllable, ISnapshotBuilderTile {
    public static final IdAllocator IDS = TileBC.IDS.makeChild("filler");
    public static final int NET_TASKS = IDS.allocId("TASKS");

    /** The blocks to place: only block items are accepted. */
    public final ItemStackHandler invResources = new ItemStackHandler(27) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return stack.getItem() instanceof BlockItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final MjBattery battery = new MjBattery(16_000 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));
    public final FullStatement<IFillerPattern> patternStatement = new FullStatement<>(FillerType.INSTANCE, 4);
    private final SnapshotBuilder builder = new SnapshotBuilder(this);
    @Nullable
    private Box box;
    private boolean canExcavate = true;
    private boolean inverted = false;
    private Mode mode = Mode.ON;
    /** Set when a gate sets the pattern: the GUI shows that it is locked for a short while. */
    private int lockedTicks;
    private boolean targetDirty = true;
    private boolean hadTasks;

    public TileFiller(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.FILLER_TILE.get(), pos, state);
        caps.addProvider(mjCaps);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, new InsertOnlyItemHandler(invResources),
            EnumPipePart.VALUES);
        caps.addCapabilityInstance(TilesAPI.CAP_CONTROLLABLE, this, EnumPipePart.VALUES);
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

    // Setting up the box

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level.isClientSide) {
            return;
        }
        // A box of connected markers right behind the filler gives it its box
        Direction facing = getBlockState().getValue(BlockFiller.FACING);
        if (level.getBlockEntity(worldPosition.relative(facing.getOpposite())) instanceof TileMarkerVolume marker) {
            Box markerBox = marker.getBox();
            if (markerBox != null) {
                marker.removeConnectedMarkers();
                setBox(markerBox);
            }
        }
    }

    /** Sets the box to fill (or none). */
    public void setBox(@Nullable Box box) {
        this.box = box;
        targetDirty = true;
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    @Nullable
    public Box getBox() {
        return box;
    }

    public SnapshotBuilder getBuilder() {
        return builder;
    }

    @Override
    public MjBattery getBattery() {
        return battery;
    }

    // Inventory

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
        for (int slot = 0; slot < invResources.getSlots(); slot++) {
            drops.add(invResources.getStackInSlot(slot));
        }
        return drops;
    }

    // Settings

    @Override
    public boolean canExcavate() {
        return canExcavate;
    }

    public void setCanExcavate(boolean canExcavate) {
        this.canExcavate = canExcavate;
        setChanged();
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
        targetDirty = true;
        setChanged();
    }

    public boolean isLocked() {
        return lockedTicks > 0;
    }

    /** Changes the pattern (from the GUI). */
    public void setPatternFromGui(@Nullable IFillerPattern pattern, IStatementParameter[] params) {
        patternStatement.set(pattern, params);
        targetDirty = true;
        setChanged();
    }

    /** Works out the template again (for a new pattern, box, or inversion). */
    private void updateTarget() {
        targetDirty = false;
        Template template = null;
        IFillerPattern pattern = patternStatement.get();
        if (box != null && pattern != null) {
            IStatementParameter[] params = patternStatement.getParameters().clone();
            IFilledTemplate filled = pattern.createTemplate(this, params);
            if (filled instanceof Template t) {
                template = t;
                if (inverted) {
                    template.invert();
                }
            }
        }
        builder.setTarget(template == null ? null
            : new TemplateTarget(template.data, template.getSize(), template.min != null ? template.min : box.min(),
                Rotation.NONE));
    }

    // ITickable

    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }
        battery.tick(level, worldPosition);
        if (lockedTicks > 0) {
            lockedTicks--;
        }
        if (targetDirty) {
            updateTarget();
        }
        if (mode != Mode.OFF) {
            builder.tick();
        }
        boolean hasTasks = !builder.breakTasks.isEmpty() || !builder.placeTasks.isEmpty();
        if (hasTasks || hadTasks) {
            createAndSendMessage(NET_TASKS, builder::writeTasks);
        }
        hadTasks = hasTasks;
    }

    // IFillerStatementContainer

    @Override
    public BlockEntity getTile() {
        return this;
    }

    @Override
    public Level getFillerWorld() {
        return level;
    }

    @Override
    public boolean hasBox() {
        return box != null;
    }

    @Override
    public BlockPos getBoxMin() {
        if (box == null) {
            throw new IllegalStateException("The filler has no box!");
        }
        return box.min();
    }

    @Override
    public BlockPos getBoxSize() {
        if (box == null) {
            throw new IllegalStateException("The filler has no box!");
        }
        return box.max().subtract(box.min()).offset(1, 1, 1);
    }

    @Override
    public void setPattern(IFillerPattern pattern, IStatementParameter[] params) {
        if (patternStatement.get() != pattern || !Arrays.equals(patternStatement.getParameters(), params)) {
            patternStatement.set(pattern, params);
            targetDirty = true;
        }
        lockedTicks = 3;
    }

    // IControllable

    @Override
    public Mode getControlMode() {
        return mode;
    }

    @Override
    public void setControlMode(Mode mode) {
        this.mode = mode;
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (box == null) {
                player.displayClientMessage(Component.translatable("buildcraft.filler.no_box"), true);
            } else {
                openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                    (id, inventory, p) -> new ContainerFiller(id, p, this));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("battery", battery.serializeNBT());
        nbt.put("inv", invResources.serializeNBT());
        nbt.putBoolean("canExcavate", canExcavate);
        nbt.putBoolean("inverted", inverted);
        nbt.putString("mode", mode.name());
        if (box != null) {
            nbt.put("box", box.save());
        }
        nbt.put("pattern", patternStatement.writeToNbt());
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
        invResources.deserializeNBT(nbt.getCompound("inv"));
        canExcavate = nbt.getBoolean("canExcavate");
        inverted = nbt.getBoolean("inverted");
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException e) {
            mode = Mode.ON;
        }
        box = nbt.contains("box") ? Box.load(nbt.getCompound("box")) : null;
        patternStatement.readFromNbt(nbt.getCompound("pattern"));
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
                buffer.writeBoolean(inverted);
                buffer.writeBoolean(lockedTicks > 0);
                buffer.writeEnum(mode);
                buffer.writeVarInt(builder.leftToBreak);
                buffer.writeVarInt(builder.leftToPlace);
                buffer.writeBoolean(builder.isFinished());
                patternStatement.writeToBuffer(buffer);
            }
        }
    }

    /** Set on the client from the GUI data. */
    public boolean clientFinished;

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                box = Box.read(buffer);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                canExcavate = buffer.readBoolean();
                inverted = buffer.readBoolean();
                lockedTicks = buffer.readBoolean() ? 1 : 0;
                mode = buffer.readEnum(Mode.class);
                builder.leftToBreak = buffer.readVarInt();
                builder.leftToPlace = buffer.readVarInt();
                clientFinished = buffer.readBoolean();
                patternStatement.readFromBuffer(buffer);
            } else if (id == NET_TASKS) {
                builder.readTasks(buffer);
            }
        }
    }

    // Rendering

    @Override
    public AABB getRenderBoundingBox() {
        AABB bb = new AABB(worldPosition);
        return box == null ? bb : bb.minmax(box.toAABB());
    }
}
