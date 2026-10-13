/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.PluggableLens;

/** A lens or a filter, of one colour (or clear). It can only go on item pipes. */
public class ItemPluggableLens extends Item implements IItemPluggable {
    @Nullable
    public final DyeColor colour;
    public final boolean isFilter;

    public ItemPluggableLens(Properties properties, @Nullable DyeColor colour, boolean isFilter) {
        super(properties);
        this.colour = colour;
        this.isFilter = isFilter;
    }

    /** @return The name of the item for the given colour (null for clear) and kind. */
    public static String getItemName(@Nullable DyeColor colour, boolean isFilter) {
        return (isFilter ? "filter_" : "lens_") + (colour == null ? "clear" : colour.getName());
    }

    @Nullable
    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        if (holder.getPipe() == null || !(holder.getPipe().getFlow() instanceof IFlowItems)) {
            return null;
        }
        holder.getPipeWorld().playSound(null, holder.getPipePos(), SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1, 1);
        return new PluggableLens(BCSiliconPlugs.LENS, holder, side, colour, isFilter);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component colourName = colour == null ? Component.translatable("buildcraft.lens.clear")
            : Component.translatable("color.minecraft." + colour.getName());
        return Component.translatable(isFilter ? "buildcraft.lens.filter" : "buildcraft.lens.lens", colourName);
    }
}
