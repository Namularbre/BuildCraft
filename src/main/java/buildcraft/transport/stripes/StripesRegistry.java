/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import buildcraft.api.core.EnumHandlerPriority;
import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IStripesHandlerItem;
import buildcraft.api.transport.IStripesRegistry;

public enum StripesRegistry implements IStripesRegistry {
    INSTANCE;

    private final EnumMap<EnumHandlerPriority, List<IStripesHandlerItem>> itemHandlers =
        new EnumMap<>(EnumHandlerPriority.class);

    StripesRegistry() {
        for (EnumHandlerPriority priority : EnumHandlerPriority.VALUES) {
            itemHandlers.put(priority, new ArrayList<>());
        }
    }

    @Override
    public void addHandler(IStripesHandlerItem handler, EnumHandlerPriority priority) {
        itemHandlers.get(priority).add(handler);
    }

    @Override
    public boolean handleItem(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        for (EnumHandlerPriority priority : EnumHandlerPriority.VALUES) {
            for (IStripesHandlerItem handler : itemHandlers.get(priority)) {
                if (handler.handle(level, pos, direction, stack, player, activator)) {
                    return true;
                }
            }
        }
        return false;
    }
}
