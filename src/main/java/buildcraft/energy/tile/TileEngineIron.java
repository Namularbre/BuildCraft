/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager.IDirtyFuel;
import buildcraft.api.fuels.ISolidCoolant;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase;

import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.container.ContainerEngineIron;

/** The combustion engine: burns liquid fuels, and needs a coolant to avoid overheating. Some fuels leave residue,
 * which has to be taken out of the engine. */
public class TileEngineIron extends TileEngineBase {
    public static final int MAX_FLUID = 10_000;

    public static final double COOLDOWN_RATE = 0.05;
    public static final int MAX_COOLANT_PER_TICK = 40;

    public final FluidTank tankFuel = new EngineTank(this::isValidFuel);
    public final FluidTank tankCoolant = new EngineTank(this::isValidCoolant);
    public final FluidTank tankResidue = new EngineTank(this::isResidue);
    /** What pipes and buckets see: fuel and coolant go in, and residue comes out. */
    private final IFluidHandler fluidHandler = new InternalFluidHandler();

    private int penaltyCooling = 0;
    private boolean lastPowered = false;
    private double burnTime;
    private double residueAmount = 0;
    private IFuel currentFuel;

    public TileEngineIron(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_IRON_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.FLUID_HANDLER, fluidHandler, EnumPipePart.VALUES);
    }

    private class EngineTank extends FluidTank {
        EngineTank(java.util.function.Predicate<FluidStack> validator) {
            super(MAX_FLUID, validator);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    // Saving and syncing

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("tankFuel", tankFuel.writeToNBT(new CompoundTag()));
        nbt.put("tankCoolant", tankCoolant.writeToNBT(new CompoundTag()));
        nbt.put("tankResidue", tankResidue.writeToNBT(new CompoundTag()));
        nbt.putInt("penaltyCooling", penaltyCooling);
        nbt.putDouble("burnTime", burnTime);
        nbt.putDouble("residueAmount", residueAmount);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        tankFuel.readFromNBT(nbt.getCompound("tankFuel"));
        tankCoolant.readFromNBT(nbt.getCompound("tankCoolant"));
        tankResidue.readFromNBT(nbt.getCompound("tankResidue"));
        penaltyCooling = nbt.getInt("penaltyCooling");
        burnTime = nbt.getDouble("burnTime");
        residueAmount = nbt.getDouble("residueAmount");
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && (id == NET_GUI_DATA || id == NET_GUI_TICK)) {
            buffer.writeFluidStack(tankFuel.getFluid());
            buffer.writeFluidStack(tankCoolant.getFluid());
            buffer.writeFluidStack(tankResidue.getFluid());
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && (id == NET_GUI_DATA || id == NET_GUI_TICK)) {
            tankFuel.setFluid(buffer.readFluidStack());
            tankCoolant.setFluid(buffer.readFluidStack());
            tankResidue.setFluid(buffer.readFluidStack());
        }
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            if (addSolidCoolant(player, held)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (FluidUtil.interactWithFluidHandler(player, hand, level, worldPosition, hit.getDirection())) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerEngineIron(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Melts a solid coolant (like ice) straight into the coolant tank. */
    private boolean addSolidCoolant(Player player, ItemStack held) {
        ISolidCoolant solid = BuildcraftFuelRegistry.coolant.getSolidCoolant(held);
        if (solid == null) {
            return false;
        }
        ItemStack single = held.copyWithCount(1);
        FluidStack melted = solid.getFluidFromSolidCoolant(single);
        if (melted == null || melted.isEmpty()
            || tankCoolant.fill(melted, IFluidHandler.FluidAction.SIMULATE) != melted.getAmount()) {
            return false;
        }
        if (!level.isClientSide) {
            tankCoolant.fill(melted, IFluidHandler.FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return true;
    }

    // Engine overrides

    @Override
    public double getPistonSpeed() {
        switch (getPowerStage()) {
            case BLUE:
                return 0.04;
            case GREEN:
                return 0.05;
            case YELLOW:
                return 0.06;
            case RED:
                return 0.07;
            default:
                return 0;
        }
    }

    @Nonnull
    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return !tankFuel.isEmpty() && penaltyCooling == 0 && isRedstonePowered;
    }

    @Override
    protected void burn() {
        final FluidStack fuel = tankFuel.getFluid();
        if (currentFuel == null || !currentFuel.getFluid().isFluidEqual(fuel)) {
            currentFuel = BuildcraftFuelRegistry.fuel.getFuel(fuel);
        }

        if (fuel.isEmpty() || currentFuel == null) {
            return;
        }

        if (penaltyCooling <= 0) {
            if (isRedstonePowered) {
                lastPowered = true;

                if (burnTime > 0 || fuel.getAmount() > 0) {
                    if (burnTime > 0) {
                        burnTime--;
                    }
                    if (burnTime <= 0) {
                        if (fuel.getAmount() > 0) {
                            tankFuel.drain(1, IFluidHandler.FluidAction.EXECUTE);
                            burnTime += currentFuel.getTotalBurningTime() / 1000.0;

                            // If we also produce residue then put it out too
                            if (currentFuel instanceof IDirtyFuel dirtyFuel) {
                                FluidStack residueFluid = dirtyFuel.getResidue().copy();
                                residueAmount += residueFluid.getAmount() / 1000.0;
                                if (residueAmount >= 1) {
                                    residueFluid.setAmount(Mth.floor(residueAmount));
                                    residueAmount -= tankResidue.fill(residueFluid, IFluidHandler.FluidAction.EXECUTE);
                                }
                            }
                        } else {
                            tankFuel.setFluid(FluidStack.EMPTY);
                            currentFuel = null;
                            currentOutput = 0;
                            return;
                        }
                    }
                    currentOutput = currentFuel.getPowerPerCycle();
                    addPower(currentFuel.getPowerPerCycle());
                    heat += currentFuel.getPowerPerCycle() * HEAT_PER_MJ / MjAPI.MJ;
                }
            } else if (lastPowered) {
                lastPowered = false;
                // 10 ticks of penalty on top of the cooling
                penaltyCooling = 10;
            }
        }
    }

    @Override
    public void updateHeatLevel() {
        double target;
        if (heat > MIN_HEAT && (penaltyCooling > 0 || !isRedstonePowered)) {
            heat -= COOLDOWN_RATE;
            target = MIN_HEAT;
        } else if (heat > IDEAL_HEAT) {
            target = IDEAL_HEAT;
        } else {
            target = heat;
        }

        if (target != heat) {
            double extraHeat = heat - target;
            if (extraHeat > 0 && tankCoolant.getFluidAmount() > 0) {
                float coolPerMb = BuildcraftFuelRegistry.coolant.getDegreesPerMb(tankCoolant.getFluid(), (float) heat);
                if (coolPerMb > 0) {
                    int coolantAmount = Math.min(MAX_COOLANT_PER_TICK, tankCoolant.getFluidAmount());
                    heat -= coolantAmount * coolPerMb;
                    tankCoolant.drain(coolantAmount, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            getPowerStage();
        }

        if (heat <= MIN_HEAT && penaltyCooling > 0) {
            penaltyCooling--;
        }

        if (heat <= MIN_HEAT) {
            heat = MIN_HEAT;
        }
    }

    @Override
    public boolean isActive() {
        return penaltyCooling <= 0;
    }

    @Override
    public long getMaxPower() {
        return 10_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerReceived() {
        return 2_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 500 * MjAPI.MJ;
    }

    @Override
    public float explosionRange() {
        return 4;
    }

    @Override
    protected int getMaxChainLength() {
        return 4;
    }

    @Override
    public long getCurrentOutput() {
        return currentFuel == null ? 0 : currentFuel.getPowerPerCycle();
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    // Fluid related

    private boolean isValidFuel(FluidStack fluid) {
        return BuildcraftFuelRegistry.fuel.getFuel(fluid) != null;
    }

    private boolean isValidCoolant(FluidStack fluid) {
        return BuildcraftFuelRegistry.coolant.getCoolant(fluid) != null;
    }

    private boolean isResidue(FluidStack fluid) {
        // The client doesn't know the current fuel, so it trusts the server
        if (level != null && level.isClientSide) {
            return true;
        }
        if (currentFuel instanceof IDirtyFuel dirtyFuel) {
            return fluid.isFluidEqual(dirtyFuel.getResidue());
        }
        return false;
    }

    private class InternalFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 3;
        }

        private FluidTank tank(int index) {
            return index == 0 ? tankFuel : index == 1 ? tankCoolant : tankResidue;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return MAX_FLUID;
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            return tank != 2 && tank(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = tankFuel.fill(resource, action);
            if (filled == 0) {
                filled = tankCoolant.fill(resource, action);
            }
            return filled;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tankResidue.drain(resource, action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tankResidue.drain(maxDrain, action);
        }
    }
}
