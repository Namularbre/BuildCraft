package buildcraft.api.transport.pipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.core.IFluidFilter;

public interface IFlowFluid {
    /** @param millibuckets The maximum amount of fluid to extract.
     * @param from The side of the pipe that the tank to extract from is on.
     * @param filter The fluidstack that the extracted fluid must match, or {@link FluidStack#EMPTY} for any fluid.
     * @return The fluidstack extracted and inserted into the pipe, or {@link FluidStack#EMPTY} if nothing was. */
    @Nonnull
    FluidStack tryExtractFluid(int millibuckets, Direction from, @Nonnull FluidStack filter, boolean simulate);

    /** Advanced version of {@link #tryExtractFluid(int, Direction, FluidStack, boolean)}, which extracts the first
     * fluid that matches the given filter.
     *
     * @return The fluidstack extracted and inserted into the pipe, or {@link FluidStack#EMPTY} if nothing was. */
    @Nonnull
    FluidStack tryExtractFluidAdv(int millibuckets, Direction from, IFluidFilter filter, boolean simulate);

    /** Attempts to insert a fluid directly into the pipe. Note that this will fail if the pipe currently contains a
     * different fluid type.
     *
     * @param from The side that the fluid should *not* go in, or null if the fluid may flow in any direction.
     * @return The amount of fluid that was accepted, or 0 if no fluid was accepted. */
    int insertFluidsForce(@Nonnull FluidStack fluid, @Nullable Direction from, boolean simulate);

    /** Tries to extract fluids directly from the pipe. NOTE: This is intended for {@link PipeBehaviour} implementors
     * ONLY! This will result in very buggy behaviour if external tiles try to use this!
     *
     * @param min The minimum amount of fluid to extract. If less than this amount is in the given center then nothing
     *            will be extracted.
     * @param section The section to extract from. Null means the center.
     * @return The fluid extracted, or {@link FluidStack#EMPTY} if nothing was. */
    @Nonnull
    FluidStack extractFluidsForce(int min, int max, @Nullable Direction section, boolean simulate);
}
