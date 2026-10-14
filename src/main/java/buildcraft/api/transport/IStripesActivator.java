/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Something that uses items in the world (usually a stripes pipe), and that items can be given back to. */
public interface IStripesActivator {
    /** Sends the given item back through the stripes pipe.
     *
     * @return True if it was sent, false if it should be dropped instead. */
    boolean sendItem(@Nonnull ItemStack itemStack, Direction from);

    void dropItem(@Nonnull ItemStack itemStack, Direction from);
}
