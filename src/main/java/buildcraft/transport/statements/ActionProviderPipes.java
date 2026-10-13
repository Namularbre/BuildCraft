/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IActionProvider;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.transport.IWireEmitter;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventStatement;

/** Lets the pipe (and what is on it) add actions to its gates. */
public enum ActionProviderPipes implements IActionProvider {
    INSTANCE;

    @Override
    public void addInternalActions(Collection<IActionInternal> actions, IStatementContainer container) {
        if (container instanceof IGate gate) {
            IPipeHolder holder = gate.getPipeHolder();
            holder.fireEvent(new PipeEventStatement.AddActionInternal(holder, actions));
            if (container instanceof IWireEmitter) {
                for (DyeColor colour : DyeColor.values()) {
                    if (TriggerPipeSignal.doesGateHaveColour(gate, colour)) {
                        actions.add(BCTransportStatements.ACTION_PIPE_SIGNAL[colour.ordinal()]);
                    }
                }
            }
        }
    }

    @Override
    public void addInternalSidedActions(Collection<IActionInternalSided> actions, IStatementContainer container,
        @Nonnull Direction side) {
        if (container instanceof IGate gate) {
            IPipeHolder holder = gate.getPipeHolder();
            holder.fireEvent(new PipeEventStatement.AddActionInternalSided(holder, actions, side));
        }
    }

    @Override
    public void addExternalActions(Collection<IActionExternal> actions, @Nonnull Direction side, BlockEntity tile) {}
}
