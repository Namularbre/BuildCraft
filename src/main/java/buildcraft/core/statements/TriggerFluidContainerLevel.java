/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.StatementParameterItemStack;

/** Active when the tank next to the gate is filled below a given level (with any fluid, or the given one). */
public class TriggerFluidContainerLevel extends BCStatement implements ITriggerExternal {
    public final TriggerType type;

    public TriggerFluidContainerLevel(TriggerType type) {
        super("buildcraft:fluid." + type.name().toLowerCase(Locale.ROOT),
            "buildcraft.fluid." + type.name().toLowerCase(Locale.ROOT));
        this.type = type;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_liquidcontainer_" + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.fluidlevel.below", (int) (type.level * 100));
    }

    @Override
    public boolean isTriggerActive(BlockEntity tile, Direction side, IStatementContainer statementContainer,
        IStatementParameter[] parameters) {
        IFluidHandler handler = tile.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
        if (handler == null) {
            return false;
        }
        FluidStack searchedFluid = TriggerFluidContainer.getSearchedFluid(parameters);
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            if (fluid.isEmpty()) {
                return searchedFluid == null || handler.fill(searchedFluid, FluidAction.SIMULATE) > 0;
            }
            if (searchedFluid == null || searchedFluid.isFluidEqual(fluid)) {
                return fluid.getAmount() / (float) handler.getTankCapacity(i) < type.level;
            }
        }
        return false;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_FLUID_ALL;
    }

    public enum TriggerType {
        BELOW25(0.25F),
        BELOW50(0.5F),
        BELOW75(0.75F);

        public final float level;

        TriggerType(float level) {
            this.level = level;
        }
    }
}
