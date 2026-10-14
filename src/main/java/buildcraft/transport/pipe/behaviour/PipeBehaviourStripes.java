/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeEventActionActivate;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeEventStatement;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.misc.BlockUtil;

import buildcraft.factory.tile.TileMiningWell;
import buildcraft.transport.statements.BCTransportStatements;

/** Faces away from its only connection: breaks the block there when powered (sending what it drops back through the
 * pipes), and uses the items that reach its end there (placing blocks, planting seeds, extending itself with pipes,
 * and so on). */
public class PipeBehaviourStripes extends PipeBehaviour implements IStripesActivator, IMjRedstoneReceiver {
    private final MjBattery battery = new MjBattery(256 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(this);

    @Nullable
    public Direction direction = null;
    private long progress;

    public PipeBehaviourStripes(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourStripes(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        battery.deserializeNBT(nbt.getCompound("battery"));
        direction = nbt.contains("direction") ? Direction.byName(nbt.getString("direction")) : null;
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("battery", battery.serializeNBT());
        if (direction != null) {
            nbt.putString("direction", direction.getName());
        }
        return nbt;
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(buffer, side);
        buffer.writeByte(direction == null ? -1 : direction.ordinal());
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(buffer, side);
        int dir = buffer.readByte();
        direction = dir < 0 ? null : Direction.values()[dir];
    }

    private void setDirection(@Nullable Direction newValue) {
        if (direction != newValue) {
            direction = newValue;
            if (!pipe.getHolder().getPipeWorld().isClientSide) {
                pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
            }
        }
    }

    // Actions

    @PipeEventHandler
    public void addInternalActions(PipeEventStatement.AddActionInternal event) {
        for (Direction face : Direction.values()) {
            if (!pipe.isConnected(face)) {
                PipePluggable plug = pipe.getHolder().getPluggable(face);
                if (plug == null || !plug.isBlocking()) {
                    event.actions.add(BCTransportStatements.ACTION_PIPE_DIRECTION[face.ordinal()]);
                }
            }
        }
    }

    @PipeEventHandler
    public void onActionActivate(PipeEventActionActivate event) {
        for (Direction face : Direction.values()) {
            if (event.action == BCTransportStatements.ACTION_PIPE_DIRECTION[face.ordinal()]) {
                setDirection(face);
            }
        }
    }

    // IMjRedstoneReceiver

    @Override
    public boolean canConnect(@Nonnull IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return battery.getCapacity() - battery.getStored();
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        return battery.addPowerChecking(microJoules, simulate);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return mjCaps.getCapability(capability, facing);
    }

    // Stripes

    @Override
    public boolean canConnect(Direction face, PipeBehaviour other) {
        return !(other instanceof PipeBehaviourStripes);
    }

    @Override
    public void onTick() {
        Level level = pipe.getHolder().getPipeWorld();
        BlockPos pos = pipe.getHolder().getPipePos();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // Face away from the only connection (unless a gate chose another free side)
        if (direction == null || pipe.isConnected(direction)) {
            int sides = 0;
            Direction dir = null;
            for (Direction face : Direction.values()) {
                if (pipe.isConnected(face)) {
                    sides++;
                    dir = face;
                }
            }
            setDirection(sides == 1 ? dir.getOpposite() : null);
        }
        battery.tick(level, pos);
        if (direction == null) {
            progress = 0;
            return;
        }
        BlockPos front = pos.relative(direction);
        if (!BlockUtil.canMachineBreak(level, front) || level.getBlockState(front).getFluidState().isSource()) {
            progress = 0;
            return;
        }
        long target = TileMiningWell.getBreakPower(server, front);
        int breakerId = pos.hashCode();
        if (progress < target) {
            progress += battery.extractPower(0, Math.min(target - progress, MjAPI.MJ * 10));
            if (progress > 0) {
                level.destroyBlockProgress(breakerId, front, (int) (progress * 9 / target));
            }
        } else {
            level.destroyBlockProgress(breakerId, front, -1);
            Direction dir = direction;
            BlockUtil.breakBlockAndGetDrops(server, front, pipe.getHolder().getOwner())
                .ifPresent(stacks -> stacks.forEach(stack -> sendItem(stack, dir)));
            progress = 0;
        }
    }

    /** Uses the items that reach the end of the pipe (instead of dropping them), if a handler knows what to do with
     * them. */
    @PipeEventHandler
    public void onDrop(PipeEventItem.Drop event) {
        if (direction == null || !(pipe.getHolder().getPipeWorld() instanceof ServerLevel level)) {
            return;
        }
        IPipeHolder holder = pipe.getHolder();
        BlockPos pos = holder.getPipePos();
        FakePlayer player = FakePlayerFactory.get(level, holder.getOwner());
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() + 0.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, event.getStack());
        if (PipeApi.stripeRegistry.handleItem(level, pos, direction, event.getStack(), player, this)) {
            event.setStack(ItemStack.EMPTY);
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    sendItem(stack, direction);
                }
            }
        }
        inventory.clearContent();
    }

    @Override
    public void dropItem(@Nonnull ItemStack stack, Direction direction) {
        BlockPos pos = pipe.getHolder().getPipePos();
        Block.popResource(pipe.getHolder().getPipeWorld(), pos, stack);
    }

    @Override
    public boolean sendItem(@Nonnull ItemStack stack, Direction from) {
        if (pipe.getFlow() instanceof IFlowItems flow) {
            flow.insertItemsForce(stack, from, null, 0.02);
            return true;
        }
        return false;
    }
}
