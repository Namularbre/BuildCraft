/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import buildcraft.api.core.EnumHandlerPriority;

/** The handlers that stripes pipes use items with. */
public interface IStripesRegistry {
    /** Adds a handler with a {@link EnumHandlerPriority} of {@linkplain EnumHandlerPriority#NORMAL NORMAL}. */
    default void addHandler(IStripesHandlerItem handler) {
        addHandler(handler, EnumHandlerPriority.NORMAL);
    }

    void addHandler(IStripesHandlerItem handler, EnumHandlerPriority priority);

    /** @param pos The position of the stripes pipe.
     * @return True if a handler handled the itemstack, false otherwise (and so nothing has been done) */
    boolean handleItem(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator);
}
