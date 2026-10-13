/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeEventActionActivate;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeEventStatement;

import buildcraft.transport.statements.ActionPipeColor;
import buildcraft.transport.statements.BCTransportStatements;

/** Sends the items of its colour to the side it faces, and the other items anywhere else. The wrench changes the
 * colour when used on the centre (or on the side it faces), and the side it faces when used on another side. */
public class PipeBehaviourDaizuli extends PipeBehaviourDirectional {
    private DyeColor colour = DyeColor.WHITE;

    public PipeBehaviourDaizuli(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDaizuli(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        colour = DyeColor.byName(nbt.getString("colour"), DyeColor.WHITE);
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.putString("colour", colour.getName());
        return nbt;
    }

    @Override
    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(buffer, side);
        buffer.writeByte(colour.getId());
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        super.readPayload(buffer, side);
        colour = DyeColor.byId(buffer.readUnsignedByte());
    }

    public DyeColor getColour() {
        return colour;
    }

    public void setColour(DyeColor colour) {
        if (this.colour != colour) {
            this.colour = colour;
            pipe.getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
        }
    }

    /** The side it faces shows the colour, the other sides are filled. */
    @Override
    public int getTextureIndex(@Nullable Direction face) {
        if (face != null && face != currentDir.face) {
            return 16;
        }
        return colour.getId();
    }

    @Override
    protected boolean canFaceDirection(Direction dir) {
        return dir != null;
    }

    @Override
    public boolean onPipeActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ,
        EnumPipePart part) {
        if (part != EnumPipePart.CENTER && part != currentDir) {
            return super.onPipeActivate(player, trace, hitX, hitY, hitZ, part);
        }
        return PipeBehaviourLapis.useWrenchToChangeColour(player, trace, colour, this::setColour);
    }

    @PipeEventHandler
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {
        Direction dir = getCurrentDir();
        if (dir == null) {
            return;
        }
        if (colour == sideCheck.colour) {
            sideCheck.disallowAllExcept(dir);
        } else {
            sideCheck.disallow(dir);
        }
    }

    @Override
    public void addActions(PipeEventStatement.AddActionInternal event) {
        super.addActions(event);
        Collections.addAll(event.actions, BCTransportStatements.ACTION_PIPE_COLOUR);
    }

    @Override
    public void onActionActivate(PipeEventActionActivate event) {
        super.onActionActivate(event);
        if (event.action instanceof ActionPipeColor action) {
            setColour(action.colour);
        }
    }
}
