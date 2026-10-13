/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.transport.IWireManager;

import buildcraft.core.statements.BCStatement;

/** Whether the wires of a colour in the pipe of the gate are powered (or not). The parameters check more colours. */
public class TriggerPipeSignal extends BCStatement implements ITriggerInternal {
    private final boolean active;
    private final DyeColor colour;

    public TriggerPipeSignal(boolean active, DyeColor colour) {
        super("buildcraft:pipe.wire.input." + colour.getName() + (active ? ".active" : ".inactive"),
            "buildcraft.pipe.wire.input." + colour.getName() + (active ? ".active" : ".inactive"));
        this.active = active;
        this.colour = colour;
    }

    public static boolean doesGateHaveColour(IGate gate, DyeColor colour) {
        return gate.getPipeHolder().getWireManager().hasPartOfColor(colour);
    }

    /** @return The texture of a pipe signal (an "on" one is lit). */
    public static ResourceLocation getSprite(boolean active, DyeColor colour) {
        return sprite("trigger_pipesignal_" + colour.getName() + (active ? "_active" : "_inactive"));
    }

    public static Component getDescription(boolean active, DyeColor colour) {
        return Component.translatable("buildcraft.gate.trigger.pipe.wire." + (active ? "active" : "inactive"),
            Component.translatable("color.minecraft." + colour.getName()));
    }

    @Override
    public int maxParameters() {
        return 3;
    }

    @Override
    public Component getDescription() {
        return getDescription(active, colour);
    }

    @Override
    public boolean isTriggerActive(IStatementContainer container, IStatementParameter[] parameters) {
        if (!(container instanceof IGate gate)) {
            return false;
        }
        IWireManager wires = gate.getPipeHolder().getWireManager();
        if (active != wires.isAnyPowered(colour)) {
            return false;
        }
        for (IStatementParameter param : parameters) {
            if (param instanceof TriggerParameterSignal signal && signal.colour != null) {
                if (signal.active != wires.isAnyPowered(signal.colour)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return TriggerParameterSignal.EMPTY;
    }

    @Override
    public ResourceLocation getSprite() {
        return getSprite(active, colour);
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.TRIGGER_PIPE_SIGNAL;
    }
}
