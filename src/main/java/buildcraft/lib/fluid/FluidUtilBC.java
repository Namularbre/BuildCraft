/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class FluidUtilBC {
    private FluidUtilBC() {}

    /** Offers the contents of the given tank to every neighbouring fluid handler. */
    public static void pushFluidAround(Level level, BlockPos pos, IFluidHandler tank) {
        for (Direction side : Direction.values()) {
            FluidStack toPush = tank.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            if (toPush.isEmpty()) {
                return;
            }
            BlockPos neighbourPos = pos.relative(side);
            if (!level.isLoaded(neighbourPos)) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(neighbourPos);
            if (neighbour == null) {
                continue;
            }
            IFluidHandler handler = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite())
                .orElse(null);
            if (handler == null) {
                continue;
            }
            int filled = handler.fill(toPush, FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, FluidAction.EXECUTE);
            }
        }
    }

    /** @return A bucket of the fluid of the source block at the given position, or {@link FluidStack#EMPTY} if there
     *         isn't a source block (or it can't be picked up). */
    @Nonnull
    public static FluidStack getSourceFluid(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.isEmpty() || !fluidState.isSource()) {
            return FluidStack.EMPTY;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BucketPickup)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
    }

    /** Removes the source block at the given position, like a bucket would. */
    public static void drainSourceBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LiquidBlock || !(state.getBlock() instanceof BucketPickup pickup)) {
            // Liquid blocks are simply removed (picking them up would also play a sound, for every block pumped)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            // Waterlogged blocks
            pickup.pickupBlock(level, pos, state);
        }
    }
}
