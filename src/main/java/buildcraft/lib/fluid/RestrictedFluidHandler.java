/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import javax.annotation.Nonnull;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** What other blocks see of a machine's tank: they may only be allowed to fill it, or to drain it. */
public final class RestrictedFluidHandler implements IFluidHandler {
    private final FluidTank tank;
    private final boolean canFill, canDrain;

    private RestrictedFluidHandler(FluidTank tank, boolean canFill, boolean canDrain) {
        this.tank = tank;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    public static RestrictedFluidHandler fillOnly(FluidTank tank) {
        return new RestrictedFluidHandler(tank, true, false);
    }

    public static RestrictedFluidHandler drainOnly(FluidTank tank) {
        return new RestrictedFluidHandler(tank, false, true);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int index, @Nonnull FluidStack stack) {
        return canFill && tank.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return canFill ? tank.fill(resource, action) : 0;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return canDrain ? tank.drain(resource, action) : FluidStack.EMPTY;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return canDrain ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
    }
}
