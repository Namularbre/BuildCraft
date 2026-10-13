/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate;

/** A gate, ready to be placed on a pipe. Every variant is a separate item. */
public class ItemPluggableGate extends Item implements IItemPluggable {
    public final GateVariant variant;

    public ItemPluggableGate(Properties properties, GateVariant variant) {
        super(properties);
        this.variant = variant;
    }

    @Nullable
    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        SoundType sound = variant.material().block.defaultBlockState().getSoundType();
        holder.getPipeWorld().playSound(null, holder.getPipePos(), sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        return new PluggableGate(BCSiliconPlugs.GATE, holder, side, variant);
    }

    @Override
    public Component getName(ItemStack stack) {
        return variant.getDisplayName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
        TooltipFlag flag) {
        tooltip.add(Component.translatable("buildcraft.gate.slots", variant.numSlots())
            .withStyle(ChatFormatting.GRAY));
        if (variant.numTriggerArgs() == variant.numActionArgs()) {
            if (variant.numTriggerArgs() > 0) {
                tooltip.add(Component.translatable("buildcraft.gate.params", variant.numTriggerArgs())
                    .withStyle(ChatFormatting.GRAY));
            }
        } else {
            if (variant.numTriggerArgs() > 0) {
                tooltip.add(Component.translatable("buildcraft.gate.params.trigger", variant.numTriggerArgs())
                    .withStyle(ChatFormatting.GRAY));
            }
            if (variant.numActionArgs() > 0) {
                tooltip.add(Component.translatable("buildcraft.gate.params.action", variant.numActionArgs())
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
