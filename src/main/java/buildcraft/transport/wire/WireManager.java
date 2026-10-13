/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.WireNode;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;

/** The wires that go through a single pipe. */
public class WireManager implements IWireManager {
    private final IPipeHolder holder;
    public final Map<EnumWirePart, DyeColor> parts = new EnumMap<>(EnumWirePart.class);
    /** The parts that are powered, as the server last sent them. */
    public final Set<EnumWirePart> poweredClient = EnumSet.noneOf(EnumWirePart.class);
    public final Map<EnumWireBetween, DyeColor> betweens = new EnumMap<>(EnumWireBetween.class);

    public WireManager(IPipeHolder holder) {
        this.holder = holder;
    }

    @Override
    public IPipeHolder getHolder() {
        return holder;
    }

    private Level getLevel() {
        return holder.getPipeWorld();
    }

    @Override
    public boolean addPart(EnumWirePart part, DyeColor colour) {
        if (parts.containsKey(part)) {
            return false;
        }
        parts.put(part, colour);
        onChanged();
        return true;
    }

    @Nullable
    @Override
    public DyeColor removePart(EnumWirePart part) {
        DyeColor colour = parts.remove(part);
        if (colour != null) {
            onChanged();
        }
        return colour;
    }

    public void removeParts(Collection<EnumWirePart> toRemove) {
        toRemove.forEach(parts::remove);
        onChanged();
    }

    private void onChanged() {
        if (getLevel() != null && !getLevel().isClientSide) {
            WireSystems.invalidate(getLevel());
            holder.getPipeTile().setChanged();
            holder.scheduleNetworkUpdate(IPipeHolder.PipeMessageReceiver.WIRES);
        }
        updateBetweens(false);
    }

    /** Finds which wires are drawn between the parts (in this pipe, and to the next ones). */
    @Override
    public void updateBetweens(boolean recursive) {
        betweens.clear();
        if (parts.isEmpty() || getLevel() == null) {
            return;
        }
        for (EnumWireBetween between : EnumWireBetween.VALUES) {
            DyeColor colour = parts.get(between.parts[0]);
            if (colour == null) {
                continue;
            }
            if (between.to == null) {
                if (parts.get(between.parts[1]) == colour) {
                    betweens.put(between, colour);
                }
            } else if (WireSystems.canWireConnect(holder, between.to)) {
                IPipe pipe = holder.getNeighbourPipe(between.to);
                if (pipe != null && pipe.getHolder().getWireManager().getColorOfPart(between.parts[1]) == colour) {
                    betweens.put(between, colour);
                }
            }
        }
    }

    @Nullable
    @Override
    public DyeColor getColorOfPart(EnumWirePart part) {
        return parts.get(part);
    }

    @Override
    public boolean hasPartOfColor(DyeColor color) {
        return parts.containsValue(color);
    }

    @Override
    public boolean isPowered(EnumWirePart part) {
        if (getLevel().isClientSide) {
            return poweredClient.contains(part);
        }
        return parts.containsKey(part)
            && WireSystems.get(getLevel()).isPowered(new WireNode(holder.getPipePos(), part));
    }

    @Override
    public boolean isAnyPowered(DyeColor color) {
        for (Map.Entry<EnumWirePart, DyeColor> entry : parts.entrySet()) {
            if (entry.getValue() == color && isPowered(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    /** @return The parts that are powered right now. Server side only. */
    public Set<EnumWirePart> getPoweredParts() {
        Set<EnumWirePart> powered = EnumSet.noneOf(EnumWirePart.class);
        for (EnumWirePart part : parts.keySet()) {
            if (isPowered(part)) {
                powered.add(part);
            }
        }
        return powered;
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        int[] wiresArray = new int[parts.size() * 2];
        int i = 0;
        for (Map.Entry<EnumWirePart, DyeColor> entry : parts.entrySet()) {
            wiresArray[i++] = entry.getKey().ordinal();
            wiresArray[i++] = entry.getValue().getId();
        }
        nbt.putIntArray("parts", wiresArray);
        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        parts.clear();
        int[] wiresArray = nbt.getIntArray("parts");
        for (int i = 0; i + 1 < wiresArray.length; i += 2) {
            if (wiresArray[i] >= 0 && wiresArray[i] < EnumWirePart.VALUES.length) {
                parts.put(EnumWirePart.VALUES[wiresArray[i]], DyeColor.byId(wiresArray[i + 1]));
            }
        }
    }

    /** Writes the wires, and which of them are powered. */
    public void writePayload(FriendlyByteBuf buffer, Set<EnumWirePart> powered) {
        buffer.writeByte(parts.size());
        for (Map.Entry<EnumWirePart, DyeColor> entry : parts.entrySet()) {
            buffer.writeByte(entry.getKey().ordinal());
            buffer.writeByte(entry.getValue().getId());
            buffer.writeBoolean(powered.contains(entry.getKey()));
        }
    }

    public void readPayload(FriendlyByteBuf buffer) {
        parts.clear();
        poweredClient.clear();
        int count = buffer.readUnsignedByte();
        for (int i = 0; i < count; i++) {
            EnumWirePart part = EnumWirePart.VALUES[buffer.readUnsignedByte()];
            parts.put(part, DyeColor.byId(buffer.readUnsignedByte()));
            if (buffer.readBoolean()) {
                poweredClient.add(part);
            }
        }
        updateBetweens(false);
    }
}
