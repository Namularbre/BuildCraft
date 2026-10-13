/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IFlowFluid;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventStatement;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.statement.BCSiliconStatements;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;

/** Powers the pipe it is on (like a redstone engine would) when it is turned on by hand or by a gate. */
public class PluggablePulsar extends PipePluggable {
    private static final int PULSE_STAGE = 20;
    private static final AABB[] BOXES = PluggableGate.boxes();

    private boolean manuallyEnabled = false;
    /** Increments from 0 to {@link #PULSE_STAGE} to decide when it should pulse some power into the pipe behaviour */
    private int pulseStage = 0;
    private int gateEnabledTicks;
    private int gateSinglePulses;
    private boolean lastPulsing = false;
    /** Used on the client to determine if this should render pulsing */
    private boolean isPulsing = false;

    public PluggablePulsar(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    // Saving + Loading

    public PluggablePulsar(PluggableDefinition definition, IPipeHolder holder, Direction side, CompoundTag nbt) {
        super(definition, holder, side);
        manuallyEnabled = nbt.getBoolean("manuallyEnabled");
        gateEnabledTicks = nbt.getInt("gateEnabledTicks");
        gateSinglePulses = nbt.getInt("gateSinglePulses");
        pulseStage = Mth.clamp(nbt.getInt("pulseStage"), 0, PULSE_STAGE);
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.putBoolean("manuallyEnabled", manuallyEnabled);
        nbt.putInt("gateEnabledTicks", gateEnabledTicks);
        nbt.putInt("gateSinglePulses", gateSinglePulses);
        nbt.putInt("pulseStage", pulseStage);
        return nbt;
    }

    // Networking

    public PluggablePulsar(PluggableDefinition definition, IPipeHolder holder, Direction side,
        FriendlyByteBuf buffer) {
        super(definition, holder, side);
        readData(buffer);
    }

    @Override
    public void writeCreationPayload(FriendlyByteBuf buffer) {
        writeData(buffer);
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        if (side == LogicalSide.CLIENT) {
            readData(buffer);
            holder.scheduleRenderUpdate();
        }
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        if (side == LogicalSide.SERVER) {
            writeData(buffer);
        }
    }

    private void writeData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isPulsing());
        buffer.writeBoolean(manuallyEnabled);
    }

    private void readData(FriendlyByteBuf buffer) {
        isPulsing = buffer.readBoolean();
        manuallyEnabled = buffer.readBoolean();
    }

    // PipePluggable

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.PLUG_PULSAR.get());
    }

    @Override
    public void onTick() {
        if (holder.getPipeWorld().isClientSide) {
            return;
        }
        boolean isOn = isPulsing();
        pulseStage = isOn ? pulseStage + 1 : 0;
        if (gateEnabledTicks > 0) {
            gateEnabledTicks--;
        }
        if (pulseStage == PULSE_STAGE) {
            pulseStage = 0;
            if (holder.getPipe() != null && holder.getPipe().getBehaviour() instanceof IMjRedstoneReceiver receiver) {
                pulse(receiver);
            }
        }
        if (isOn != lastPulsing) {
            lastPulsing = isOn;
            scheduleNetworkUpdate();
        }
    }

    private void pulse(IMjRedstoneReceiver receiver) {
        if (gateSinglePulses > 0) {
            // A single pulse moves a single item (or a bucket of fluid)
            long power = MjAPI.MJ;
            if (holder.getPipe().getFlow() instanceof IFlowFluid) {
                power = PipeBehaviourWood.MJ_PER_MILLIBUCKET * 1000;
            } else if (holder.getPipe().getFlow() instanceof IFlowItems) {
                power = PipeBehaviourWood.MJ_PER_ITEM;
            }
            long excess = receiver.receivePower(power, true);
            if (excess == 0) {
                receiver.receivePower(power, false);
                gateSinglePulses--;
            }
            // Otherwise nothing was extracted, so try again next time
        } else {
            receiver.receivePower(MjAPI.MJ, false);
        }
    }

    @PipeEventHandler
    public void onAddActions(PipeEventStatement.AddActionInternalSided event) {
        if (event.side == this.side) {
            event.actions.add(BCSiliconStatements.ACTION_PULSAR_CONSTANT);
            event.actions.add(BCSiliconStatements.ACTION_PULSAR_SINGLE);
        }
    }

    @Override
    public boolean onPluggableActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ) {
        if (!holder.getPipeWorld().isClientSide) {
            manuallyEnabled = !manuallyEnabled;
            holder.getPipeWorld().playSound(null, holder.getPipePos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3f, manuallyEnabled ? 0.6f : 0.5f);
            scheduleNetworkUpdate();
        }
        return true;
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new KeyPlugPulsar(side, isPulsing);
    }

    /** Keeps the pulsar on for the next half second. */
    public void enablePulsar() {
        gateEnabledTicks = 10;
    }

    public void addSinglePulse() {
        gateSinglePulses++;
    }

    public boolean isManuallyEnabled() {
        return manuallyEnabled;
    }

    private boolean isPulsing() {
        return manuallyEnabled || gateEnabledTicks > 0 || gateSinglePulses > 0;
    }

    public static final class KeyPlugPulsar extends PluggableModelKey {
        public final boolean on;

        public KeyPlugPulsar(Direction side, boolean on) {
            super(side);
            this.on = on;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && ((KeyPlugPulsar) obj).on == on;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), on);
        }
    }
}
