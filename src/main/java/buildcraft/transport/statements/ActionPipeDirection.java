/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.core.statements.BCStatement;

/** Turns a directional pipe (like a wooden or iron pipe) to the given side. The pipe does it when it sees the
 * action. */
public class ActionPipeDirection extends BCStatement implements IActionInternal {
    public final Direction direction;

    public ActionPipeDirection(Direction direction) {
        super("buildcraft:pipe.dir." + direction.getSerializedName(),
            "buildcraft.pipe.dir." + direction.getSerializedName());
        this.direction = direction;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.action.pipe.direction",
            Component.translatable("buildcraft.direction." + direction.getSerializedName()));
    }

    @Override
    public IStatement rotateLeft() {
        Direction face = direction.getAxis() == Direction.Axis.Y ? direction : direction.getClockWise();
        return BCTransportStatements.ACTION_PIPE_DIRECTION[face.ordinal()];
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        // The pipe behaviour listens for this
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_dir_" + direction.getSerializedName());
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.ACTION_PIPE_DIRECTION;
    }
}
