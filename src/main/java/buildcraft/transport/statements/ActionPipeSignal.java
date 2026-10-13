/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.IWireEmitter;

import buildcraft.core.statements.BCStatement;

/** Powers the wires of a colour (and the colours of the parameters) in the pipe of the gate. */
public class ActionPipeSignal extends BCStatement implements IActionInternal {
    public final DyeColor colour;

    public ActionPipeSignal(DyeColor colour) {
        super("buildcraft:pipe.wire.output." + colour.getName(), "buildcraft.pipe.wire.output." + colour.getName());
        this.colour = colour;
    }

    public static Component getDescription(DyeColor colour) {
        return Component.translatable("buildcraft.gate.action.pipe.wire",
            Component.translatable("color.minecraft." + colour.getName()));
    }

    @Override
    public Component getDescription() {
        return getDescription(colour);
    }

    @Override
    public int maxParameters() {
        return 3;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return ActionParameterSignal.EMPTY;
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {
        if (!(container instanceof IWireEmitter emitter)) {
            return;
        }
        emitter.emitWire(colour);
        for (IStatementParameter param : parameters) {
            if (param instanceof ActionParameterSignal signal && signal.colour != null) {
                emitter.emitWire(signal.colour);
            }
        }
    }

    @Override
    public ResourceLocation getSprite() {
        return TriggerPipeSignal.getSprite(true, colour);
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.ACTION_PIPE_SIGNAL;
    }
}
