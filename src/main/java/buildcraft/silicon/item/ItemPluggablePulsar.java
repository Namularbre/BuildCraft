/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.PluggablePulsar;

/** A pulsar: it can only go on pipes that take redstone engine power (like wooden pipes). */
public class ItemPluggablePulsar extends Item implements IItemPluggable {
    public ItemPluggablePulsar(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        if (holder.getPipe() == null || !(holder.getPipe().getBehaviour() instanceof IMjRedstoneReceiver)) {
            return null;
        }
        holder.getPipeWorld().playSound(null, holder.getPipePos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1, 1);
        return new PluggablePulsar(BCSiliconPlugs.PULSAR, holder, side);
    }
}
