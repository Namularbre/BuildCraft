/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IControllable.Mode;
import buildcraft.api.tiles.TilesAPI;

/** Turns the machine next to the gate on or off, or makes it loop. */
public class ActionMachineControl extends BCStatement implements IActionExternal {
    public final Mode mode;

    public ActionMachineControl(Mode mode) {
        super("buildcraft:machine." + mode.lowerCaseName, "buildcraft.machine." + mode.lowerCaseName);
        this.mode = mode;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.action.machine." + mode.lowerCaseName);
    }

    @Override
    public void actionActivate(BlockEntity target, Direction side, IStatementContainer source,
        IStatementParameter[] parameters) {
        IControllable controllable =
            target.getCapability(TilesAPI.CAP_CONTROLLABLE, side.getOpposite()).orElse(null);
        if (controllable != null && controllable.acceptsControlMode(mode)) {
            controllable.setControlMode(mode);
        }
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("action_machinecontrol_" + mode.lowerCaseName);
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.ACTION_MACHINE_CONTROL;
    }
}
