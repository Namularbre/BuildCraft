/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;

/** An item that is placed on pipes as a pluggable without any data. */
public class ItemPluggableSimple extends Item implements IItemPluggable {
    private final PluggableDefinition definition;

    public ItemPluggableSimple(Properties properties, PluggableDefinition definition) {
        super(properties);
        if (definition.creator == null) {
            throw new IllegalArgumentException("The definition needs a creator: " + definition.identifier);
        }
        this.definition = definition;
    }

    @Nullable
    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        holder.getPipeWorld().playSound(null, holder.getPipePos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1, 1);
        return definition.creator.createSimplePluggable(definition, holder, side);
    }
}
