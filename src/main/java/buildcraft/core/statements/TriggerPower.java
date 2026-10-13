/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;

/** Active when a block holding MJ is (almost) full or (almost) empty. */
public class TriggerPower extends BCStatement implements ITriggerInternal, ITriggerExternal {
    private final boolean high;

    public TriggerPower(boolean high) {
        super("buildcraft:energyStored" + (high ? "high" : "low"));
        this.high = high;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite(high ? "trigger_energy_storage_high" : "trigger_energy_storage_low");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.machine.energyStored." + (high ? "high" : "low"));
    }

    public boolean isTriggeredMjConnector(@Nullable IMjReadable readable) {
        if (readable == null) {
            return false;
        }
        long stored = readable.getStored();
        long max = readable.getCapacity();
        if (max > 0) {
            double level = stored / (double) max;
            return high ? level > 0.95 : level < 0.05;
        }
        return false;
    }

    public static boolean isTriggeringTile(@Nullable BlockEntity tile, @Nullable Direction face) {
        return tile != null && tile.getCapability(MjAPI.CAP_READABLE, face).isPresent();
    }

    protected boolean isActive(BlockEntity tile, @Nullable Direction side) {
        return isTriggeredMjConnector(tile.getCapability(MjAPI.CAP_READABLE, side).orElse(null));
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        return isActive(source.getTile(), null);
    }

    @Override
    public boolean isTriggerActive(BlockEntity target, Direction side, IStatementContainer source,
        IStatementParameter[] parameters) {
        return isActive(target, side.getOpposite());
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_POWER;
    }
}
