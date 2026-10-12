/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui.slot;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** A slot that only shows a copy of an item (for example as part of a recipe): clicking it with an item sets a copy of
 * it, and clicking it with nothing clears it. Items never go in or out. Clicks are handled by
 * {@link buildcraft.lib.gui.ContainerBC}. */
public class SlotPhantom extends SlotItemHandler {
    public SlotPhantom(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    /** Sets the shown item to a single copy of the given one (or clears it). */
    public void setPhantom(ItemStack stack) {
        set(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }
}
