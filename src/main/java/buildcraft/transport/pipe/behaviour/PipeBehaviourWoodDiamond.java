/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.IFluidFilter;
import buildcraft.api.core.IStackFilter;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;

import buildcraft.lib.tile.TileBC;

import buildcraft.transport.container.ContainerDiamondWoodPipe;

/** A wooden pipe that only extracts what its 9 filters allow (or don't allow), or each filter in turn. */
public class PipeBehaviourWoodDiamond extends PipeBehaviourWood {
    public static final int FILTER_COUNT = 9;

    public enum FilterMode {
        WHITE_LIST,
        BLACK_LIST,
        /** Extracts what matches the current filter, then moves to the next one (only for items). */
        ROUND_ROBIN;

        public static final FilterMode[] VALUES = values();

        public static FilterMode get(int index) {
            return index >= 0 && index < VALUES.length ? VALUES[index] : WHITE_LIST;
        }
    }

    public final ItemStackHandler filters = new ItemStackHandler(FILTER_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            onSlotChanged(slot);
        }
    };
    public FilterMode filterMode = FilterMode.WHITE_LIST;
    public int currentFilter = 0;
    /** Whether {@link #currentFilter} is a filter that is set (so false if there aren't any). */
    public boolean filterValid = false;

    public PipeBehaviourWoodDiamond(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourWoodDiamond(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        filters.deserializeNBT(nbt.getCompound("filters"));
        filterMode = FilterMode.get(nbt.getByte("mode"));
        currentFilter = Math.floorMod(nbt.getByte("currentFilter"), FILTER_COUNT);
        filterValid = hasFilters();
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("filters", filters.serializeNBT());
        nbt.putByte("mode", (byte) filterMode.ordinal());
        nbt.putByte("currentFilter", (byte) currentFilter);
        return nbt;
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(buffer, side);
        buffer.writeByte(filterMode.ordinal());
        buffer.writeByte(currentFilter);
        buffer.writeBoolean(filterValid);
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(buffer, side);
        filterMode = FilterMode.get(buffer.readUnsignedByte());
        currentFilter = buffer.readUnsignedByte() % FILTER_COUNT;
        filterValid = buffer.readBoolean();
    }

    public void setFilterMode(FilterMode mode) {
        if (mode == FilterMode.ROUND_ROBIN && !(pipe.getFlow() instanceof IFlowItems)) {
            // Fluids can't be extracted one filter at a time
            return;
        }
        filterMode = mode;
        pipe.getHolder().getPipeTile().setChanged();
        pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
    }

    @Override
    public boolean onPipeActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ,
        EnumPipePart part) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof IToolWrench || player.getOffhandItem().getItem() instanceof IToolWrench) {
            return super.onPipeActivate(player, trace, hitX, hitY, hitZ, part);
        }
        if (held.getItem() instanceof IItemPluggable) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && pipe.getHolder().getPipeTile() instanceof TileBC tile) {
            tile.openMenu(serverPlayer, Component.translatable("buildcraft.gui.diamond_wood_pipe"),
                (id, inventory, p) -> new ContainerDiamondWoodPipe(id, p, tile));
        }
        return true;
    }

    private boolean hasFilters() {
        for (int i = 0; i < FILTER_COUNT; i++) {
            if (!filters.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void onSlotChanged(int slot) {
        pipe.getHolder().getPipeTile().setChanged();
        if (!filters.getStackInSlot(slot).isEmpty()) {
            if (!filterValid) {
                currentFilter = slot;
                filterValid = true;
            }
        } else if (slot == currentFilter) {
            advanceFilter();
        }
    }

    private boolean matchesAnyFilter(ItemStack stack) {
        for (int i = 0; i < FILTER_COUNT; i++) {
            ItemStack filter = filters.getStackInSlot(i);
            if (!filter.isEmpty() && ItemStack.isSameItemSameTags(filter, stack)) {
                return true;
            }
        }
        return false;
    }

    private IStackFilter getStackFilter() {
        return switch (filterMode) {
            case WHITE_LIST -> hasFilters() ? this::matchesAnyFilter : stack -> true;
            case BLACK_LIST -> stack -> !matchesAnyFilter(stack);
            case ROUND_ROBIN -> stack -> {
                ItemStack filter = filters.getStackInSlot(currentFilter);
                return !filter.isEmpty() && ItemStack.isSameItemSameTags(filter, stack);
            };
        };
    }

    @Override
    protected int extractItems(IFlowItems flow, Direction dir, int count, boolean simulate) {
        if (filters.getStackInSlot(currentFilter).isEmpty()) {
            advanceFilter();
        }
        int extracted = flow.tryExtractItems(1, dir, null, getStackFilter(), simulate);
        if (extracted > 0 && filterMode == FilterMode.ROUND_ROBIN && !simulate) {
            advanceFilter();
        }
        return extracted;
    }

    private boolean matchesAnyFilter(FluidStack fluid) {
        for (int i = 0; i < FILTER_COUNT; i++) {
            FluidStack filter = FluidUtil.getFluidContained(filters.getStackInSlot(i)).orElse(FluidStack.EMPTY);
            if (!filter.isEmpty() && filter.isFluidEqual(fluid)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    protected FluidStack extractFluid(IFlowFluid flow, Direction dir, int millibuckets, boolean simulate) {
        IFluidFilter filter = switch (filterMode) {
            case WHITE_LIST -> hasFilters() ? this::matchesAnyFilter : fluid -> true;
            case BLACK_LIST -> fluid -> !matchesAnyFilter(fluid);
            case ROUND_ROBIN -> null;
        };
        if (filter == null) {
            return FluidStack.EMPTY;
        }
        FluidStack extracted = flow.tryExtractFluidAdv(millibuckets, dir, filter, simulate);
        return extracted == null ? FluidStack.EMPTY : extracted;
    }

    private void advanceFilter() {
        int lastFilter = currentFilter;
        filterValid = false;
        for (int i = 0; i < FILTER_COUNT; i++) {
            currentFilter = (currentFilter + 1) % FILTER_COUNT;
            if (!filters.getStackInSlot(currentFilter).isEmpty()) {
                filterValid = true;
                break;
            }
        }
        if (!filterValid) {
            currentFilter = lastFilter;
        }
        if (lastFilter != currentFilter && pipe.getHolder().getPipeWorld() != null
            && !pipe.getHolder().getPipeWorld().isClientSide) {
            pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        }
    }
}
