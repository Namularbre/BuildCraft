/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.StatementParameterItemStack;

/** Checks whether the tank next to the gate is empty, full, has some space, or contains some fluid. */
public class TriggerFluidContainer extends BCStatement implements ITriggerExternal {
    public final State state;

    public TriggerFluidContainer(State state) {
        super("buildcraft:fluid." + state.name().toLowerCase(Locale.ROOT),
            "buildcraft.fluid." + state.name().toLowerCase(Locale.ROOT));
        this.state = state;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_liquidcontainer_" + state.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public int maxParameters() {
        return state == State.CONTAINS || state == State.SPACE ? 1 : 0;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.fluid." + state.name().toLowerCase(Locale.ROOT));
    }

    /** @return A single mB of the fluid held by the item of the first parameter, or null if there isn't one. */
    @Nullable
    static FluidStack getSearchedFluid(IStatementParameter[] parameters) {
        if (parameters == null || parameters.length < 1 || parameters[0] == null) {
            return null;
        }
        ItemStack stack = parameters[0].getItemStack();
        if (stack.isEmpty()) {
            return null;
        }
        FluidStack fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        return fluid.isEmpty() ? null : new FluidStack(fluid, 1);
    }

    @Override
    public boolean isTriggerActive(BlockEntity tile, Direction side, IStatementContainer statementContainer,
        IStatementParameter[] parameters) {
        IFluidHandler handler = tile.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
        if (handler == null || handler.getTanks() == 0) {
            return false;
        }
        FluidStack searchedFluid = getSearchedFluid(parameters);
        switch (state) {
            case EMPTY:
                return handler.drain(1, FluidAction.SIMULATE).isEmpty();
            case CONTAINS:
                for (int i = 0; i < handler.getTanks(); i++) {
                    FluidStack fluid = handler.getFluidInTank(i);
                    if (!fluid.isEmpty() && (searchedFluid == null || searchedFluid.isFluidEqual(fluid))) {
                        return true;
                    }
                }
                return false;
            case SPACE:
                if (searchedFluid == null) {
                    for (int i = 0; i < handler.getTanks(); i++) {
                        if (handler.getFluidInTank(i).getAmount() < handler.getTankCapacity(i)) {
                            return true;
                        }
                    }
                    return false;
                }
                return handler.fill(searchedFluid, FluidAction.SIMULATE) > 0;
            case FULL:
                for (int i = 0; i < handler.getTanks(); i++) {
                    if (handler.getFluidInTank(i).getAmount() < handler.getTankCapacity(i)) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_FLUID_ALL;
    }

    public enum State {
        EMPTY,
        CONTAINS,
        SPACE,
        FULL
    }
}
