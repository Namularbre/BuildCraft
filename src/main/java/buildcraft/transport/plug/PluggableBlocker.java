/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.plug;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.transport.BCTransportItems;

/** Closes a side of a pipe: it doesn't connect there anymore. */
public class PluggableBlocker extends PipePluggable {
    private static final AABB[] BOXES = PlugBoxes.make(2, 4, 4);

    public PluggableBlocker(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCTransportItems.PLUG_BLOCKER.get());
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new Key(side);
    }

    public static final class Key extends PluggableModelKey {
        public Key(Direction side) {
            super(side);
        }
    }
}
