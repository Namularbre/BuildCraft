/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager.IDistillationRecipe;

import buildcraft.lib.fluid.RestrictedFluidHandler;
import buildcraft.lib.misc.data.AverageLong;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.BCFactoryBlocks;

/** Splits oil and fuels into a gas part (sent out of the top) and a liquid part (sent out of the bottom), using MJ.
 * Fluids to distill go in through the sides. */
public class TileDistiller extends TileBC implements ITickable {
    public static final int CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    public static final long MAX_MJ_PER_TICK = 6 * MjAPI.MJ;
    /** Minimum number of ticks between two updates sent to clients. */
    private static final int NET_UPDATE_DELAY = 4;

    public final FluidTank tankIn = new FluidTank(CAPACITY, TileDistiller::isDistillable) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    public final FluidTank tankGasOut = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    public final FluidTank tankLiquidOut = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };

    private final MjBattery battery = new MjBattery(1024 * MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));

    private long distillPower = 0;
    private boolean isActive = false;
    private final AverageLong powerAvg = new AverageLong(100);
    /** The average power sent to clients, rounded to half MJ. */
    private long powerAvgClient;
    private boolean changedSinceNetUpdate = true;
    private long nextNetworkUpdate;

    // Client side: the animation of the two power blocks (see the 1.12 model "tiles/distiller.json")
    private double animState, animStateLast;

    public TileDistiller(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.DISTILLER_TILE.get(), pos, state);
        caps.addCapabilityInstance(
            ForgeCapabilities.FLUID_HANDLER, RestrictedFluidHandler.fillOnly(tankIn), EnumPipePart.HORIZONTALS
        );
        caps.addCapabilityInstance(
            ForgeCapabilities.FLUID_HANDLER, RestrictedFluidHandler.drainOnly(tankGasOut), EnumPipePart.UP
        );
        caps.addCapabilityInstance(
            ForgeCapabilities.FLUID_HANDLER, RestrictedFluidHandler.drainOnly(tankLiquidOut), EnumPipePart.DOWN
        );
        caps.addProvider(mjCaps);
    }

    private static boolean isDistillable(FluidStack fluid) {
        return getRecipe(fluid) != null;
    }

    @Nullable
    private static IDistillationRecipe getRecipe(FluidStack fluid) {
        return BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry().getRecipeForInput(fluid);
    }

    private void contentsChanged() {
        changedSinceNetUpdate = true;
        setChanged();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            tickAnimation();
            return;
        }
        powerAvg.tick();

        IDistillationRecipe recipe = getRecipe(tankIn.getFluid());
        if (recipe != null && canDistill(recipe)) {
            // Uses less power when the battery is low
            long max = MAX_MJ_PER_TICK;
            max *= battery.getStored() + max;
            max /= battery.getCapacity() / 2;
            max = Math.min(max, MAX_MJ_PER_TICK);
            long power = battery.extractPower(0, max);
            powerAvg.push(max);
            distillPower += power;
            setActive(power > 0);
            if (distillPower >= recipe.powerRequired()) {
                setActive(true);
                distillPower -= recipe.powerRequired();
                tankIn.drain(recipe.in(), FluidAction.EXECUTE);
                tankGasOut.fill(recipe.outGas(), FluidAction.EXECUTE);
                tankLiquidOut.fill(recipe.outLiquid(), FluidAction.EXECUTE);
            }
        } else {
            // Give back the power spent on the unfinished operation
            battery.addPowerChecking(distillPower, false);
            distillPower = 0;
            setActive(false);
        }

        long roundedAvg = roundPower(powerAvg.getAverageLong());
        changedSinceNetUpdate |= roundedAvg != powerAvgClient;
        long now = level.getGameTime();
        if (changedSinceNetUpdate && now >= nextNetworkUpdate) {
            powerAvgClient = roundedAvg;
            nextNetworkUpdate = now + NET_UPDATE_DELAY;
            changedSinceNetUpdate = false;
            sendNetworkUpdate(NET_RENDER_DATA);
        }
    }

    private boolean canDistill(IDistillationRecipe recipe) {
        FluidStack in = tankIn.drain(recipe.in(), FluidAction.SIMULATE);
        return in.getAmount() == recipe.in().getAmount()
            && tankGasOut.fill(recipe.outGas(), FluidAction.SIMULATE) == recipe.outGas().getAmount()
            && tankLiquidOut.fill(recipe.outLiquid(), FluidAction.SIMULATE) == recipe.outLiquid().getAmount();
    }

    private void setActive(boolean active) {
        changedSinceNetUpdate |= active != isActive;
        isActive = active;
    }

    private static long roundPower(long power) {
        long div = MjAPI.MJ / 2;
        return Math.round(power / (double) div) * div;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getPowerAverage() {
        return powerAvgClient;
    }

    private void tickAnimation() {
        animStateLast = animState;
        double powerMax = MAX_MJ_PER_TICK / MjAPI.MJ;
        double speed = isActive ? (powerAvgClient / MjAPI.MJ) * 0.06 / powerMax : 0.01;
        if (isActive) {
            animState += speed;
        } else {
            animState = animState > speed ? animState - speed : 0;
        }
        // Goes up from 0 to 1.5, then loops back to 0.5 while active
        if (animStateLast >= 1.5 && animState >= 1.5) {
            animState -= 1;
            animStateLast -= 1;
        }
    }

    /** @return How far up (from 0 to 1) the first power block is. */
    public double getPowerBlockHeight1(float partialTicks) {
        double state = getAnimState(partialTicks);
        return 1 - Math.abs((state % 1) - 0.5) * 2;
    }

    /** @return How far up (from 0 to 1) the second power block is, half a cycle behind the first one. */
    public double getPowerBlockHeight2(float partialTicks) {
        double state = getAnimState(partialTicks);
        return 1 - Math.abs((state <= 0.5 ? 0 : (state - 0.5) % 1) - 0.5) * 2;
    }

    private double getAnimState(float partialTicks) {
        return animStateLast + (animState - animStateLast) * partialTicks;
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("tankIn", tankIn.writeToNBT(new CompoundTag()));
        nbt.put("tankGasOut", tankGasOut.writeToNBT(new CompoundTag()));
        nbt.put("tankLiquidOut", tankLiquidOut.writeToNBT(new CompoundTag()));
        nbt.put("battery", battery.serializeNBT());
        nbt.putLong("distillPower", distillPower);
        powerAvg.writeToNbt(nbt, "powerAvg");
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        tankIn.readFromNBT(nbt.getCompound("tankIn"));
        tankGasOut.readFromNBT(nbt.getCompound("tankGasOut"));
        tankLiquidOut.readFromNBT(nbt.getCompound("tankLiquidOut"));
        battery.deserializeNBT(nbt.getCompound("battery"));
        distillPower = nbt.getLong("distillPower");
        powerAvg.readFromNbt(nbt, "powerAvg");
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            buffer.writeFluidStack(tankIn.getFluid());
            buffer.writeFluidStack(tankGasOut.getFluid());
            buffer.writeFluidStack(tankLiquidOut.getFluid());
            buffer.writeBoolean(isActive);
            buffer.writeLong(powerAvgClient);
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_RENDER_DATA) {
            tankIn.setFluid(buffer.readFluidStack());
            tankGasOut.setFluid(buffer.readFluidStack());
            tankLiquidOut.setFluid(buffer.readFluidStack());
            isActive = buffer.readBoolean();
            powerAvgClient = buffer.readLong();
        }
    }
}
