/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandlerModifiable;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.IngredientStack;

import buildcraft.lib.misc.data.AverageLong;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

/** A table that works with the power of lasers. */
public abstract class TileLaserTableBase extends TileBC implements ILaserTarget, ITickable {
    /** The average power is sent to clients rounded to this. */
    private static final long MJ_FLOW_ROUND = MjAPI.MJ / 10;

    private final AverageLong avgPower = new AverageLong(120);
    public long avgPowerClient;
    public long power;

    protected TileLaserTableBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** @return The power needed to finish the current work, or 0 if there is nothing to do. */
    public abstract long getTarget();

    @Override
    public long getRequiredLaserPower() {
        return Math.max(0, getTarget() - power);
    }

    @Override
    public long receiveLaserPower(long microJoules) {
        long received = Math.min(microJoules, getRequiredLaserPower());
        power += received;
        avgPower.push(received);
        return microJoules - received;
    }

    @Override
    public boolean isInvalidTarget() {
        return isRemoved();
    }

    @Override
    public void tick() {
        avgPower.tick();
        if (level.isClientSide) {
            return;
        }
        if (getTarget() <= 0) {
            power = 0;
            avgPower.clear();
        }
    }

    /** Takes the given ingredients out of the inventory.
     *
     * @param simulate If true, nothing is taken out.
     * @return True if every ingredient was there. */
    protected static boolean extract(IItemHandlerModifiable inv, Collection<IngredientStack> items, boolean simulate) {
        for (IngredientStack definition : items) {
            int remaining = definition.count;
            for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
                ItemStack slotStack = inv.getStackInSlot(i);
                if (!slotStack.isEmpty() && definition.ingredient.test(slotStack)) {
                    int spend = Math.min(remaining, slotStack.getCount());
                    remaining -= spend;
                    if (!simulate) {
                        inv.setStackInSlot(i, slotStack.copyWithCount(slotStack.getCount() - spend));
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("power", power);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        power = nbt.getLong("power");
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_GUI_TICK) {
            buffer.writeLong(power);
            buffer.writeInt((int) Math.round(avgPower.getAverage() / MJ_FLOW_ROUND));
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_GUI_TICK) {
            power = buffer.readLong();
            avgPowerClient = buffer.readInt() * MJ_FLOW_ROUND;
        }
    }
}
