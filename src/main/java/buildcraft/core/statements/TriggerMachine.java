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

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.TilesAPI;

/** Active when the machine next to the gate has (or doesn't have) work to do. */
public class TriggerMachine extends BCStatement implements ITriggerExternal {
    public final boolean active;

    public TriggerMachine(boolean active) {
        super("buildcraft:work." + (active ? "scheduled" : "done"),
            "buildcraft.work." + (active ? "scheduled" : "done"));
        this.active = active;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.machine." + (active ? "scheduled" : "done"));
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite(active ? "trigger_machine_active" : "trigger_machine_inactive");
    }

    @Override
    public boolean isTriggerActive(BlockEntity tile, Direction side, IStatementContainer container,
        IStatementParameter[] parameters) {
        IHasWork hasWork = tile.getCapability(TilesAPI.CAP_HAS_WORK, side.getOpposite()).orElse(null);
        return hasWork != null && hasWork.hasWork() == active;
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_MACHINE;
    }
}
