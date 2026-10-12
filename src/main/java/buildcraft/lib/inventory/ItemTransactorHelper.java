/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.api.core.IStackFilter;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;

/** Moves items in and out of block entities, through Forge's {@link IItemHandler} capability. */
public final class ItemTransactorHelper {
    private ItemTransactorHelper() {}

    @Nullable
    public static IItemHandler getItemHandler(@Nullable BlockEntity tile, @Nullable Direction face) {
        if (tile == null) {
            return null;
        }
        return tile.getCapability(ForgeCapabilities.ITEM_HANDLER, face).orElse(null);
    }

    @Nullable
    public static IInjectable getInjectable(@Nullable BlockEntity tile, @Nullable Direction face) {
        if (tile == null) {
            return null;
        }
        return tile.getCapability(PipeApi.CAP_INJECTABLE, face).orElse(null);
    }

    /** Extracts between min and max items of a single kind (the first stack matching the filter), possibly from several
     * slots.
     *
     * @return The extracted stack, or {@link ItemStack#EMPTY} if less than min items could be extracted. */
    @Nonnull
    public static ItemStack extract(@Nullable IItemHandler handler, IStackFilter filter, int min, int max,
        boolean simulate) {
        if (handler == null || max <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack kind = ItemStack.EMPTY;
        int available = 0;
        for (int slot = 0; slot < handler.getSlots() && available < max; slot++) {
            ItemStack inSlot = handler.extractItem(slot, max - available, true);
            if (inSlot.isEmpty()) {
                continue;
            }
            if (kind.isEmpty()) {
                if (!filter.matches(inSlot)) {
                    continue;
                }
                kind = inSlot.copy();
                available = inSlot.getCount();
            } else if (ItemHandlerHelper.canItemStacksStack(kind, inSlot)) {
                available += inSlot.getCount();
            }
        }
        if (kind.isEmpty() || available < min) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(available, max);
        if (!simulate) {
            int left = count;
            for (int slot = 0; slot < handler.getSlots() && left > 0; slot++) {
                ItemStack inSlot = handler.getStackInSlot(slot);
                if (ItemHandlerHelper.canItemStacksStack(kind, inSlot)) {
                    left -= handler.extractItem(slot, left, false).getCount();
                }
            }
            count -= left;
        }
        return ItemHandlerHelper.copyStackWithSize(kind, count);
    }
}
