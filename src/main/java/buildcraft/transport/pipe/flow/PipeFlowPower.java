/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.math.BigInteger;
import java.util.EnumMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowPower;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeApi.PowerTransferInfo;
import buildcraft.api.transport.pipe.PipeEventPower;
import buildcraft.api.transport.pipe.PipeFlow;

import buildcraft.lib.misc.data.AverageInt;

/** Moves MJ through kinesis pipes. Each side of the pipe has a {@link Section}: power enters through one section, and
 * is split between the other sections in proportion to how much power they asked for in the previous tick. */
public class PipeFlowPower extends PipeFlow implements IFlowPower {
    private static final long DEFAULT_MAX_POWER = MjAPI.MJ * 10;
    public static final int NET_POWER_AMOUNTS = 2;

    /** Client-only: the scrolling offset of the power texture in the center of the pipe, per axis. */
    public final double[] clientDisplayFlowCentre = new double[3];

    private long maxPower = -1;
    private long powerLoss = -1;
    private long powerResistance = -1;

    private long currentWorldTime;

    private boolean isReceiver = false;
    private final EnumMap<Direction, Section> sections = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<Section>> sectionCaps = new EnumMap<>(Direction.class);

    public PipeFlowPower(IPipe pipe) {
        super(pipe);
        for (Direction face : Direction.values()) {
            Section section = new Section(face);
            sections.put(face, section);
            sectionCaps.put(face, LazyOptional.of(() -> section));
        }
    }

    public PipeFlowPower(IPipe pipe, CompoundTag nbt) {
        this(pipe);
        isReceiver = nbt.getBoolean("isReceiver");
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.putBoolean("isReceiver", isReceiver);
        return nbt;
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE) {
                for (Direction face : Direction.values()) {
                    Section s = sections.get(face);
                    buffer.writeInt(s.displayPower);
                    buffer.writeEnum(s.displayFlow);
                }
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(id, buffer, side);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_POWER_AMOUNTS || id == NET_ID_FULL_STATE) {
                for (Direction face : Direction.values()) {
                    Section s = sections.get(face);
                    s.displayPower = buffer.readInt();
                    s.displayFlow = buffer.readEnum(EnumFlow.class);
                }
            }
        }
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof PipeFlowPower;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        if (isReceiver) {
            if (oTile.getCapability(MjAPI.CAP_PASSIVE_PROVIDER, face.getOpposite()).isPresent()) {
                return true;
            }
        }
        IMjConnector receiver = oTile.getCapability(MjAPI.CAP_CONNECTOR, face.getOpposite()).orElse(null);
        return receiver != null && receiver.canConnect(sections.get(face));
    }

    @Override
    public void reconfigure() {
        PipeEventPower.Configure configure = new PipeEventPower.Configure(pipe.getHolder(), this);
        PowerTransferInfo pti = PipeApi.getPowerTransferInfo(pipe.getDefinition());
        configure.setReceiver(pti.isReceiver);
        configure.setMaxPower(pti.transferPerTick);
        configure.setPowerLoss(pti.lossPerTick);
        configure.setPowerResistance(pti.resistancePerTick);
        pipe.getHolder().fireEvent(configure);
        isReceiver = configure.isReceiver();
        maxPower = configure.getMaxPower();
        if (maxPower <= 0) {
            maxPower = DEFAULT_MAX_POWER;
        }
        powerLoss = clamp(configure.getPowerLoss(), -1, maxPower);
        powerResistance = clamp(configure.getPowerResistance(), -1, MjAPI.MJ);

        if (powerLoss < 0) {
            if (powerResistance < 0) {
                // 1% resistance
                powerResistance = MjAPI.MJ / 100;
            }
            powerLoss = maxPower * powerResistance / MjAPI.MJ;
        } else if (powerResistance < 0) {
            powerResistance = powerLoss * MjAPI.MJ / maxPower;
        }
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public long tryExtractPower(long maxExtracted, Direction from) {
        // Not implemented in 1.12 either: nothing pulls power out of passive providers yet
        return 0;
    }

    public Section getSection(Direction side) {
        return sections.get(side);
    }

    public boolean isReceiver() {
        return isReceiver;
    }

    public long getMaxPower() {
        return maxPower;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (facing == null) {
            return LazyOptional.empty();
        } else if (capability == MjAPI.CAP_RECEIVER) {
            return isReceiver ? sectionCaps.get(facing).cast() : LazyOptional.empty();
        } else if (capability == MjAPI.CAP_CONNECTOR) {
            return sectionCaps.get(facing).cast();
        } else {
            return LazyOptional.empty();
        }
    }

    @Override
    public void onTick() {
        if (maxPower == -1) {
            reconfigure();
        }
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            for (Direction face : Direction.values()) {
                Section s = sections.get(face);
                s.clientDisplayFlowLast = s.clientDisplayFlow;
                double diff = s.displayFlow.value * 2.4 * face.getAxisDirection().getStep();
                s.clientDisplayFlow += 16 + diff;
                s.clientDisplayFlow %= 16;

                int axis = face.getAxis().ordinal();
                double cVal = clientDisplayFlowCentre[axis];
                cVal += 16 + diff / 2;
                cVal %= 16;
                clientDisplayFlowCentre[axis] = cVal;
            }
            return;
        }

        EnumFlow[] lastFlows = new EnumFlow[6];
        int[] lastDisplayPower = new int[6];

        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            int i = face.ordinal();
            lastFlows[i] = s.displayFlow;
            lastDisplayPower[i] = s.displayPower;
        }

        step();

        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            if (s.internalPower > 0) {
                long totalPowerQuery = 0;
                for (Direction face2 : Direction.values()) {
                    if (face != face2) {
                        totalPowerQuery += sections.get(face2).powerQuery;
                    }
                }

                if (totalPowerQuery > 0) {
                    long unusedPowerQuery = totalPowerQuery;
                    for (Direction face2 : Direction.values()) {
                        if (face == face2) {
                            continue;
                        }
                        Section s2 = sections.get(face2);
                        if (s2.powerQuery > 0) {
                            long watts = Math.min(
                                BigInteger.valueOf(s.internalPower).multiply(BigInteger.valueOf(s2.powerQuery)).divide(
                                    BigInteger.valueOf(unusedPowerQuery)
                                ).longValue(), s.internalPower
                            );
                            unusedPowerQuery -= s2.powerQuery;
                            IPipe neighbour = pipe.getConnectedPipe(face2);
                            long leftover = watts;
                            if (
                                neighbour != null && neighbour.getFlow() instanceof PipeFlowPower oFlow && neighbour
                                    .isConnected(face2.getOpposite())
                            ) {
                                leftover = oFlow.sections.get(face2.getOpposite()).receivePowerInternal(watts);
                            } else {
                                IMjReceiver receiver = pipe.getHolder().getCapabilityFromPipe(
                                    face2, MjAPI.CAP_RECEIVER
                                );
                                if (receiver != null && receiver.canReceive()) {
                                    leftover = receiver.receivePower(watts, false);
                                }
                            }
                            long used = watts - leftover;
                            s.internalPower -= used;

                            s.powerAverage.push((int) used);
                            s2.powerAverage.push((int) used);

                            s.displayFlow = EnumFlow.OUT;
                            s2.displayFlow = EnumFlow.IN;
                        }
                    }
                }
            }
        }
        // Render compute goes here
        for (Section s : sections.values()) {
            s.powerAverage.tick();
            double value = s.powerAverage.getAverage() / maxPower;
            value = Math.sqrt(value);
            s.displayPower = (int) (value * MjAPI.MJ);
        }

        // Compute the tiles requesting power that are not power pipes
        for (Direction face : Direction.values()) {
            if (pipe.getConnectedType(face) != ConnectedType.TILE) {
                continue;
            }
            IMjReceiver recv = pipe.getHolder().getCapabilityFromPipe(face, MjAPI.CAP_RECEIVER);
            if (recv != null && recv.canReceive()) {
                long requested = recv.getPowerRequested();
                if (requested > 0) {
                    requestPower(face, requested);
                }
            }
        }

        // Sum the amount of power requested on each side
        long[] transferQuery = new long[6];
        for (Direction face : Direction.values()) {
            if (!pipe.isConnected(face)) {
                continue;
            }
            long query = 0;
            for (Direction face2 : Direction.values()) {
                if (face != face2) {
                    query += sections.get(face2).powerQuery;
                }
            }
            transferQuery[face.ordinal()] = query;
        }

        // Transfer requested power to neighbouring pipes
        for (Direction face : Direction.values()) {
            if (transferQuery[face.ordinal()] <= 0 || !pipe.isConnected(face)) {
                continue;
            }
            IPipe oPipe = pipe.getHolder().getNeighbourPipe(face);
            if (oPipe == null || !(oPipe.getFlow() instanceof PipeFlowPower oFlow)) {
                continue;
            }
            oFlow.requestPower(face.getOpposite(), transferQuery[face.ordinal()]);
        }

        // Networking
        boolean didChange = false;
        for (Direction face : Direction.values()) {
            Section s = sections.get(face);
            int i = face.ordinal();
            if (lastFlows[i] != s.displayFlow || lastDisplayPower[i] != s.displayPower) {
                didChange = true;
                break;
            }
        }

        if (didChange) {
            sendPayload(NET_POWER_AMOUNTS);
        }
    }

    /** Moves every section on to the next tick, at most once per tick (other pipes can call this first). */
    private void step() {
        long now = pipe.getHolder().getPipeWorld().getGameTime();
        if (currentWorldTime != now) {
            currentWorldTime = now;
            sections.values().forEach(Section::step);
        }
    }

    private void requestPower(Direction from, long amount) {
        step();
        sections.get(from).nextPowerQuery += amount;
    }

    public long getPowerRequested(@Nullable Direction side) {
        long req = 0;
        for (Direction face : Direction.values()) {
            if (side == null || face != side) {
                req += sections.get(face).powerQuery;
            }
        }
        return req;
    }

    public class Section implements IMjReceiver {
        public final Direction side;

        public double clientDisplayFlow, clientDisplayFlowLast;

        /** Range: 0 to {@link MjAPI#MJ} */
        public int displayPower;
        public EnumFlow displayFlow = EnumFlow.STATIONARY;
        public long nextPowerQuery;
        public long internalNextPower;
        public final AverageInt powerAverage = new AverageInt(10);

        long powerQuery;
        long internalPower;

        public Section(Direction side) {
            this.side = side;
        }

        void step() {
            powerQuery = nextPowerQuery;
            nextPowerQuery = 0;

            long next = internalPower;
            internalPower = internalNextPower;
            internalNextPower = next;
        }

        @Override
        public boolean canConnect(@Nonnull IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return PipeFlowPower.this.getPowerRequested(side);
        }

        long receivePowerInternal(long sent) {
            if (sent > 0) {
                internalNextPower += sent;
                return 0;
            }
            return sent;
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (isReceiver) {
                PipeFlowPower.this.step();
                if (!simulate) {
                    return this.receivePowerInternal(microJoules);
                }
                return 0;
            }
            return microJoules;
        }

        @Override
        public boolean canReceive() {
            return isReceiver;
        }
    }

    public enum EnumFlow {
        IN(-1),
        OUT(1),
        STATIONARY(0);

        public final int value;

        EnumFlow(int value) {
            this.value = value;
        }
    }
}
