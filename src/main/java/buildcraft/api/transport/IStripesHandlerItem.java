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

/** Uses an item that reaches the end of a stripes pipe. */
public interface IStripesHandlerItem {
    /** Called to handle the given {@link ItemStack} within the world. Note that the player's inventory will be empty,
     * except that the target stack will be set into its main hand. Any items left in the players inventory will be
     * returned back through the activator with {@link IStripesActivator#sendItem(ItemStack, Direction)}.
     *
     * @param pos The position of the stripes pipe.
     * @param direction The direction that the stripes pipe faces.
     * @return True if this used the item, false otherwise (note that this handler MUST NOT return false if it has
     *         changed the world in any way) */
    boolean handle(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator);
}
