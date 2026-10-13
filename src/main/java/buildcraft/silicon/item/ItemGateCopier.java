/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Copies the statements of a gate (by clicking on it), and pastes them on other gates. Sneaking and using it in the
 * air forgets what it copied. */
public class ItemGateCopier extends Item {
    private static final String NBT_DATA = "gate_data";

    public ItemGateCopier(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public static CompoundTag getCopiedGateData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_DATA) ? tag.getCompound(NBT_DATA) : null;
    }

    public static void setCopiedGateData(ItemStack stack, CompoundTag nbt) {
        stack.getOrCreateTag().put(NBT_DATA, nbt);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || getCopiedGateData(stack) == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            stack.removeTagKey(NBT_DATA);
            player.displayClientMessage(Component.translatable("buildcraft.chat.gate_copier.clear"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (getCopiedGateData(stack) != null) {
            tooltip.add(Component.translatable("buildcraft.item.nonclean.usage").withStyle(ChatFormatting.GRAY));
        }
    }
}
