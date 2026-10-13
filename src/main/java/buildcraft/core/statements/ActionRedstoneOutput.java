/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.statements.containers.ISidedStatementContainer;

/** Makes the container emit a full redstone signal (on every side, or only on the side of the gate). */
public class ActionRedstoneOutput extends BCStatement implements IActionInternal {
    protected ActionRedstoneOutput(String s) {
        // Used by fader output
        super(s);
    }

    public ActionRedstoneOutput() {
        super("buildcraft:redstone.output", "buildcraft.redstone.output");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.action.redstone.signal");
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return index == 0 ? StatementParamGateSideOnly.ANY : null;
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    protected boolean isSideOnly(IStatementParameter[] parameters) {
        return parameters != null && parameters.length > getRGSOSlot()
            && parameters[getRGSOSlot()] instanceof StatementParamGateSideOnly sideOnly && sideOnly.isSpecific;
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IRedstoneStatementContainer redstone) {
            Direction side = null;
            if (source instanceof ISidedStatementContainer sided && isSideOnly(parameters)) {
                side = sided.getSide();
            }
            redstone.setRedstoneOutput(side, getSignalLevel(parameters));
        }
    }

    protected int getRGSOSlot() {
        return 0;
    }

    protected int getSignalLevel(IStatementParameter[] parameters) {
        return 15;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("action_redstoneoutput");
    }

    @Override
    public <T> T convertTo(Class<T> clazz) {
        T obj = super.convertTo(clazz);
        if (obj != null) {
            return obj;
        }
        if (clazz.isInstance(BCCoreStatements.TRIGGER_REDSTONE_ACTIVE)) {
            return clazz.cast(BCCoreStatements.TRIGGER_REDSTONE_ACTIVE);
        }
        return null;
    }
}
