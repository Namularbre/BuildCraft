/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeBehaviour;

import buildcraft.lib.block.VanillaRotationHandlers;

/** A pipe that points in one direction, which the wrench can change. */
public abstract class PipeBehaviourDirectional extends PipeBehaviour {
    protected EnumPipePart currentDir = EnumPipePart.CENTER;

    public PipeBehaviourDirectional(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDirectional(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        setCurrentDir(Direction.byName(nbt.getString("currentDir")));
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        Direction dir = getCurrentDir();
        if (dir != null) {
            nbt.putString("currentDir", dir.getName());
        }
        return nbt;
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(buffer, side);
        buffer.writeEnum(currentDir);
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(buffer, side);
        currentDir = buffer.readEnum(EnumPipePart.class);
    }

    @Override
    public boolean onPipeActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ,
        EnumPipePart part) {
        InteractionHand hand = getWrenchHand(player);
        if (hand != null) {
            IToolWrench wrench = (IToolWrench) player.getItemInHand(hand).getItem();
            wrench.wrenchUsed(player, hand, player.getItemInHand(hand), trace);

            if (part == EnumPipePart.CENTER) {
                return advanceFacing();
            } else if (part.face != getCurrentDir() && canFaceDirection(part.face)) {
                setCurrentDir(part.face);
            }
            return true;
        }
        return false;
    }

    @Nullable
    private static InteractionHand getWrenchHand(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof IToolWrench) {
                return hand;
            }
        }
        return null;
    }

    @Override
    public void onTick() {
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            return;
        }

        if (!canFaceDirection(getCurrentDir())) {
            if (!advanceFacing()) {
                setCurrentDir(null);
            }
        }
    }

    protected abstract boolean canFaceDirection(Direction dir);

    /** @return True if the facing direction changed. */
    public boolean advanceFacing() {
        Direction current = currentDir.face;
        if (current == null) {
            current = Direction.UP;
        }
        for (int i = 0; i < 6; i++) {
            current = VanillaRotationHandlers.next(VanillaRotationHandlers.ROTATE_FACING, current);
            if (canFaceDirection(current)) {
                setCurrentDir(current);
                return true;
            }
        }
        return false;
    }

    @Nullable
    protected Direction getCurrentDir() {
        return currentDir.face;
    }

    protected void setCurrentDir(Direction setTo) {
        if (this.currentDir.face == setTo) {
            return;
        }
        this.currentDir = EnumPipePart.fromFacing(setTo);
        // The level is null while the pipe is being loaded
        Level level = pipe.getHolder().getPipeWorld();
        if (level != null && !level.isClientSide) {
            pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        }
    }
}
