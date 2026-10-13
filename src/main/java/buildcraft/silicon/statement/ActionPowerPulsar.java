/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.statement;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IActionSingle;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.core.statements.BCStatement;
import buildcraft.silicon.plug.PluggablePulsar;

/** Makes a pulsar pulse: all the time, or a single time. */
public class ActionPowerPulsar extends BCStatement implements IActionInternalSided, IActionSingle {
    public final boolean constant;

    public ActionPowerPulsar(boolean constant) {
        super("buildcraft:pulsar." + (constant ? "constant" : "single"),
            "buildcraft.pulsar.constant" + (constant ? "constant" : "single"));
        this.constant = constant;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(
            constant ? "buildcraft.gate.action.pulsar.constant" : "buildcraft.gate.action.pulsar.single");
    }

    @Override
    public void actionActivate(Direction side, IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IGate gate && gate.getPipeHolder().getPluggable(side) instanceof PluggablePulsar pulsar) {
            if (constant) {
                pulsar.enablePulsar();
            } else {
                pulsar.addSinglePulse();
            }
        }
    }

    @Override
    public boolean singleActionTick() {
        return !constant;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite(constant ? "action_pulsar_on" : "action_pulsar_single");
    }

    @Override
    public IStatement[] getPossible() {
        return BCSiliconStatements.ACTION_PULSAR;
    }
}
