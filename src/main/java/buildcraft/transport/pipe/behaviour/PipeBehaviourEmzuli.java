/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeEventActionActivate;
import buildcraft.api.transport.pipe.PipeEventStatement;

import buildcraft.lib.tile.TileBC;

import buildcraft.transport.container.ContainerEmzuliPipe;
import buildcraft.transport.statements.ActionExtractionPreset;
import buildcraft.transport.statements.BCTransportStatements;

/** A wooden pipe with 4 filters (each with a colour to paint the items with), that only extracts while a gate
 * activates them: each active filter is used in turn. */
public class PipeBehaviourEmzuli extends PipeBehaviourWood {
    public enum SlotIndex {
        SQUARE(DyeColor.RED),
        CIRCLE(DyeColor.GREEN),
        TRIANGLE(DyeColor.BLUE),
        CROSS(DyeColor.YELLOW);

        public static final SlotIndex[] VALUES = values();

        /** The colour of the gate action that activates the filter. */
        public final DyeColor colour;

        SlotIndex(DyeColor colour) {
            this.colour = colour;
        }

        public SlotIndex next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    /** The colour that the items extracted by each filter are painted with. */
    public final EnumMap<SlotIndex, DyeColor> slotColours = new EnumMap<>(SlotIndex.class);
    public final ItemStackHandler invFilters = new ItemStackHandler(SlotIndex.VALUES.length) {
        @Override
        protected void onContentsChanged(int slot) {
            pipe.getHolder().getPipeTile().setChanged();
        }
    };
    private final EnumSet<SlotIndex> activeSlots = EnumSet.noneOf(SlotIndex.class);
    /** How many more ticks each filter stays active (gates activate them again every tick). */
    private final byte[] activatedTtl = new byte[SlotIndex.VALUES.length];
    @Nullable
    private SlotIndex currentSlot = null;

    public PipeBehaviourEmzuli(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourEmzuli(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        invFilters.deserializeNBT(nbt.getCompound("Filters"));
        for (SlotIndex index : SlotIndex.VALUES) {
            byte c = nbt.getByte("slotColors[" + index.ordinal() + "]");
            if (c > 0 && c <= 16) {
                slotColours.put(index, DyeColor.byId(c - 1));
            }
        }
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("Filters", invFilters.serializeNBT());
        for (SlotIndex index : SlotIndex.VALUES) {
            DyeColor c = slotColours.get(index);
            nbt.putByte("slotColors[" + index.ordinal() + "]", (byte) (c == null ? 0 : c.getId() + 1));
        }
        return nbt;
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(buffer, side);
        for (SlotIndex index : SlotIndex.VALUES) {
            DyeColor colour = slotColours.get(index);
            buffer.writeByte(colour == null ? -1 : colour.getId());
        }
        int active = 0;
        for (SlotIndex index : activeSlots) {
            active |= 1 << index.ordinal();
        }
        buffer.writeByte(active);
        buffer.writeByte(currentSlot == null ? -1 : currentSlot.ordinal());
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(buffer, side);
        for (SlotIndex index : SlotIndex.VALUES) {
            int colour = buffer.readByte();
            if (colour < 0) {
                slotColours.remove(index);
            } else {
                slotColours.put(index, DyeColor.byId(colour));
            }
        }
        int active = buffer.readUnsignedByte();
        activeSlots.clear();
        for (SlotIndex index : SlotIndex.VALUES) {
            if ((active & (1 << index.ordinal())) != 0) {
                activeSlots.add(index);
            }
        }
        int current = buffer.readByte();
        currentSlot = current < 0 ? null : SlotIndex.VALUES[current];
    }

    public void setSlotColour(SlotIndex index, @Nullable DyeColor colour) {
        if (colour == null) {
            slotColours.remove(index);
        } else {
            slotColours.put(index, colour);
        }
        pipe.getHolder().getPipeTile().setChanged();
        pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
    }

    @Nullable
    public SlotIndex getCurrentSlot() {
        return currentSlot;
    }

    public EnumSet<SlotIndex> getActiveSlots() {
        return activeSlots;
    }

    @Override
    protected int extractItems(IFlowItems flow, Direction dir, int count, boolean simulate) {
        if (currentSlot == null && !activeSlots.isEmpty()) {
            currentSlot = getNextSlot();
        }
        if (currentSlot == null) {
            return 0;
        }
        ItemStack filter = invFilters.getStackInSlot(currentSlot.ordinal());
        int extracted = flow.tryExtractItems(count, dir, slotColours.get(currentSlot),
            stack -> ItemStack.isSameItemSameTags(filter, stack), simulate);
        if (extracted > 0 && !simulate) {
            currentSlot = getNextSlot();
            pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        }
        return extracted;
    }

    @Override
    public void onTick() {
        super.onTick();
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            return;
        }
        for (SlotIndex index : SlotIndex.VALUES) {
            byte ttl = activatedTtl[index.ordinal()];
            if (ttl > 0) {
                ttl--;
                activatedTtl[index.ordinal()] = ttl;
            }
            if (ttl == 0 && activeSlots.remove(index)) {
                if (currentSlot == index) {
                    currentSlot = getNextSlot();
                }
                pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
            }
        }
    }

    /** @return The next active filter (that has an item in it) after the current one, or null if there isn't one. */
    @Nullable
    private SlotIndex getNextSlot() {
        SlotIndex current = currentSlot == null ? SlotIndex.CROSS : currentSlot;
        for (int i = 0; i < SlotIndex.VALUES.length; i++) {
            current = current.next();
            if (activeSlots.contains(current) && !invFilters.getStackInSlot(current.ordinal()).isEmpty()) {
                return current;
            }
        }
        return null;
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
            tile.openMenu(serverPlayer, Component.translatable("buildcraft.gui.emzuli_pipe"),
                (id, inventory, p) -> new ContainerEmzuliPipe(id, p, tile));
        }
        return true;
    }

    @Override
    public void addActions(PipeEventStatement.AddActionInternal event) {
        super.addActions(event);
        Collections.addAll(event.actions, BCTransportStatements.ACTION_EXTRACTION_PRESET);
    }

    @Override
    public void onActionActivate(PipeEventActionActivate event) {
        super.onActionActivate(event);
        if (event.action instanceof ActionExtractionPreset preset) {
            if (activeSlots.add(preset.index)) {
                pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
            }
            activatedTtl[preset.index.ordinal()] = 2;
        }
    }
}
