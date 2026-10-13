/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.statements.containers.ISidedStatementContainer;

/** Active when the container receives (or doesn't receive) a redstone signal. */
public class TriggerRedstoneInput extends BCStatement implements ITriggerInternal {
    public final boolean active;

    public TriggerRedstoneInput(boolean active) {
        super("buildcraft:redstone.input." + (active ? "active" : "inactive"),
            "buildcraft.redstone.input." + (active ? "active" : "inactive"));
        this.active = active;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite(active ? "trigger_redstoneinput_active" : "trigger_redstoneinput_inactive");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.redstone.input." + (active ? "active" : "inactive"));
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return index == 0 ? StatementParamGateSideOnly.ANY : null;
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public boolean isTriggerActive(IStatementContainer container, IStatementParameter[] parameters) {
        if (container instanceof IRedstoneStatementContainer redstone) {
            int level = redstone.getRedstoneInput(null);
            if (parameters.length > 0 && parameters[0] instanceof StatementParamGateSideOnly sideOnly
                && sideOnly.isSpecific && container instanceof ISidedStatementContainer sided) {
                level = redstone.getRedstoneInput(sided.getSide());
            }
            return active ? level > 0 : level == 0;
        } else {
            return false;
        }
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_REDSTONE;
    }

    @Override
    public <T> T convertTo(Class<T> clazz) {
        T obj = super.convertTo(clazz);
        if (obj != null) {
            return obj;
        }
        if (active && clazz.isInstance(BCCoreStatements.ACTION_REDSTONE)) {
            return clazz.cast(BCCoreStatements.ACTION_REDSTONE);
        }
        return null;
    }
}
