/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;
import buildcraft.api.statements.ITriggerProvider;
import buildcraft.api.statements.containers.IRedstoneStatementContainer;
import buildcraft.api.tiles.TilesAPI;

public enum CoreTriggerProvider implements ITriggerProvider {
    INSTANCE;

    @Override
    public void addInternalTriggers(Collection<ITriggerInternal> res, IStatementContainer container) {
        res.add(BCCoreStatements.TRIGGER_TRUE);
        if (container instanceof IRedstoneStatementContainer) {
            res.add(BCCoreStatements.TRIGGER_REDSTONE_ACTIVE);
            res.add(BCCoreStatements.TRIGGER_REDSTONE_INACTIVE);
        }
        if (TriggerPower.isTriggeringTile(container.getTile(), null)) {
            res.add(BCCoreStatements.TRIGGER_POWER_HIGH);
            res.add(BCCoreStatements.TRIGGER_POWER_LOW);
        }
    }

    @Override
    public void addInternalSidedTriggers(Collection<ITriggerInternalSided> res, IStatementContainer container,
        @Nonnull Direction side) {}

    @Override
    public void addExternalTriggers(Collection<ITriggerExternal> res, @Nonnull Direction side, BlockEntity tile) {
        Direction from = side.getOpposite();
        if (TriggerPower.isTriggeringTile(tile, from)) {
            res.add(BCCoreStatements.TRIGGER_POWER_HIGH);
            res.add(BCCoreStatements.TRIGGER_POWER_LOW);
        }
        if (tile.getCapability(ForgeCapabilities.ITEM_HANDLER, from).isPresent()) {
            res.add(BCCoreStatements.TRIGGER_INVENTORY_EMPTY);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_SPACE);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_CONTAINS);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_FULL);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_BELOW_25);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_BELOW_50);
            res.add(BCCoreStatements.TRIGGER_INVENTORY_BELOW_75);
        }
        IFluidHandler fluids = tile.getCapability(ForgeCapabilities.FLUID_HANDLER, from).orElse(null);
        if (fluids != null && fluids.getTanks() > 0) {
            res.add(BCCoreStatements.TRIGGER_FLUID_EMPTY);
            res.add(BCCoreStatements.TRIGGER_FLUID_SPACE);
            res.add(BCCoreStatements.TRIGGER_FLUID_CONTAINS);
            res.add(BCCoreStatements.TRIGGER_FLUID_FULL);
            res.add(BCCoreStatements.TRIGGER_FLUID_BELOW_25);
            res.add(BCCoreStatements.TRIGGER_FLUID_BELOW_50);
            res.add(BCCoreStatements.TRIGGER_FLUID_BELOW_75);
        }
        if (tile.getCapability(TilesAPI.CAP_HAS_WORK, from).isPresent()) {
            res.add(BCCoreStatements.TRIGGER_MACHINE_ACTIVE);
            res.add(BCCoreStatements.TRIGGER_MACHINE_INACTIVE);
        }
        if (TriggerEnginePowerStage.isTriggeringTile(tile)) {
            for (TriggerEnginePowerStage stage : BCCoreStatements.TRIGGER_POWER_STAGES) {
                res.add(stage);
            }
        }
    }
}
