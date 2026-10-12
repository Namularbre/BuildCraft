/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;

import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.BCFactoryBlocks;

/** A tank holding 16 buckets. Tanks stacked on top of each other act as a single tank: liquids fill them from the
 * bottom, and gases from the top. */
public class TileTank extends TileBC implements ITickable {
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    /** Minimum number of ticks between two updates of the fluid sent to clients. */
    private static final int NET_UPDATE_DELAY = 4;

    public final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            fluidChanged = true;
            setChanged();
        }
    };
    /** What other blocks see: every tank in the column, as one. */
    private final IFluidHandler columnHandler = new ColumnHandler();

    private boolean fluidChanged = false;
    private long nextNetworkUpdate;
    private int lastComparatorLevel;

    // Client side: the amount shown, moving smoothly towards the amount sent by the server
    private FluidStack clientFluid = FluidStack.EMPTY;
    private int clientTarget, clientAmount, clientAmountLast;

    public TileTank(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.TANK_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.FLUID_HANDLER, columnHandler, EnumPipePart.VALUES);
    }

    public IFluidHandler getColumnHandler() {
        return columnHandler;
    }

    public int getComparatorLevel() {
        int amount = tank.getFluidAmount();
        return amount * 14 / tank.getCapacity() + (amount > 0 ? 1 : 0);
    }

    // Saving and syncing

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        tank.readFromNBT(nbt.getCompound("tank"));
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_RENDER_DATA) {
            buffer.writeFluidStack(tank.getFluid());
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_RENDER_DATA) {
            FluidStack fluid = buffer.readFluidStack();
            tank.setFluid(fluid);
            if (!fluid.isEmpty()) {
                clientFluid = fluid;
            }
            boolean first = clientTarget == 0 && clientAmount == 0;
            clientTarget = fluid.getAmount();
            if (first) {
                clientAmount = clientAmountLast = clientTarget;
            }
        }
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            clientAmountLast = clientAmount;
            int delta = clientTarget - clientAmount;
            if (delta != 0) {
                int step = Math.max(1, Math.abs(delta) / 4);
                clientAmount += Integer.signum(delta) * Math.min(step, Math.abs(delta));
            }
            return;
        }
        long now = level.getGameTime();
        if (fluidChanged && now >= nextNetworkUpdate) {
            fluidChanged = false;
            nextNetworkUpdate = now + NET_UPDATE_DELAY;
            sendNetworkUpdate(NET_RENDER_DATA);
        }
        int comparatorLevel = getComparatorLevel();
        if (comparatorLevel != lastComparatorLevel) {
            lastComparatorLevel = comparatorLevel;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // Interaction

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (!level.isClientSide) {
            balanceTankFluids();
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, worldPosition, hit.getDirection())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Moves fluids around to their preferred positions: liquids as low as possible, gases as high as possible. */
    public void balanceTankFluids() {
        List<TileTank> tanks = getTanks();
        FluidStack fluid = FluidStack.EMPTY;
        for (TileTank tile : tanks) {
            FluidStack held = tile.tank.getFluid();
            if (held.isEmpty()) {
                continue;
            }
            if (fluid.isEmpty()) {
                fluid = held;
            } else if (!fluid.isFluidEqual(held)) {
                return;
            }
        }
        if (fluid.isEmpty()) {
            return;
        }
        if (isGas(fluid)) {
            Collections.reverse(tanks);
        }
        TileTank prev = null;
        for (TileTank tile : tanks) {
            if (prev != null) {
                FluidUtil.tryFluidTransfer(prev.tank, tile.tank, Integer.MAX_VALUE, true);
            }
            prev = tile;
        }
    }

    private static boolean isGas(FluidStack fluid) {
        return fluid.getFluid().getFluidType().isLighterThanAir();
    }

    // Rendering

    /** @return The fluid to render, or {@link FluidStack#EMPTY}. Its amount is interpolated for smooth changes. */
    public FluidStack getFluidForRender(float partialTicks) {
        if (clientFluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int amount = Math.round(clientAmountLast + (clientAmount - clientAmountLast) * partialTicks);
        return amount <= 0 ? FluidStack.EMPTY : new FluidStack(clientFluid, amount);
    }

    // Tank columns

    /** Tests to see if this tank can connect to the other one, in the given direction. Addons may override this. */
    public boolean canConnectTo(TileTank other, Direction direction) {
        return true;
    }

    public static boolean canTanksConnect(TileTank from, TileTank to, Direction direction) {
        return from.canConnectTo(to, direction) && to.canConnectTo(from, direction.getOpposite());
    }

    /** @return Every tank in the same column as this one, from bottom to top. */
    public List<TileTank> getTanks() {
        Deque<TileTank> tanks = new ArrayDeque<>();
        tanks.add(this);
        TileTank prev = this;
        while (prev.getNeighbourTile(Direction.UP) instanceof TileTank up && canTanksConnect(prev, up, Direction.UP)) {
            tanks.addLast(up);
            prev = up;
        }
        prev = this;
        while (prev.getNeighbourTile(Direction.DOWN) instanceof TileTank down
            && canTanksConnect(prev, down, Direction.DOWN)) {
            tanks.addFirst(down);
            prev = down;
        }
        return new ArrayList<>(tanks);
    }

    /** The column of tanks as a single tank. */
    private class ColumnHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int index) {
            FluidStack total = FluidStack.EMPTY;
            for (TileTank t : TileTank.this.getTanks()) {
                FluidStack held = t.tank.getFluid();
                if (held.isEmpty()) {
                    continue;
                }
                if (total.isEmpty()) {
                    total = held.copy();
                } else {
                    total.grow(held.getAmount());
                }
            }
            return total;
        }

        @Override
        public int getTankCapacity(int index) {
            return TileTank.this.getTanks().size() * CAPACITY;
        }

        @Override
        public boolean isFluidValid(int index, @Nonnull FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            List<TileTank> tanks = TileTank.this.getTanks();
            for (TileTank t : tanks) {
                FluidStack current = t.tank.getFluid();
                if (!current.isEmpty() && !current.isFluidEqual(resource)) {
                    return 0;
                }
            }
            if (isGas(resource)) {
                Collections.reverse(tanks);
            }
            FluidStack left = resource.copy();
            int filled = 0;
            for (TileTank t : tanks) {
                int tankFilled = t.tank.fill(left, action);
                left.shrink(tankFilled);
                filled += tankFilled;
                if (left.isEmpty()) {
                    break;
                }
            }
            return filled;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return drain(resource::isFluidEqual, resource.getAmount(), action);
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drain(fluid -> true, maxDrain, action);
        }

        /** Drains from the top of the column for liquids, and from the bottom for gases. */
        private FluidStack drain(Predicate<FluidStack> filter, int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            List<TileTank> tanks = TileTank.this.getTanks();
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty() || !filter.test(contained)) {
                return FluidStack.EMPTY;
            }
            if (!isGas(contained)) {
                Collections.reverse(tanks);
            }
            FluidStack total = FluidStack.EMPTY;
            for (TileTank t : tanks) {
                int realMax = maxDrain - total.getAmount();
                if (realMax <= 0) {
                    break;
                }
                FluidStack drained = t.tank.drain(realMax, action);
                if (drained.isEmpty()) {
                    continue;
                }
                if (total.isEmpty()) {
                    total = drained.copy();
                } else {
                    total.grow(drained.getAmount());
                }
            }
            return total;
        }
    }
}
