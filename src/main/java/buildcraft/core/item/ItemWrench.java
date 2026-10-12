/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import buildcraft.api.blocks.CustomRotationHelper;
import buildcraft.api.tools.IToolWrench;

import buildcraft.lib.block.VanillaRotationHandlers;

public class ItemWrench extends Item implements IToolWrench {
    public ItemWrench(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canWrench(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        return true;
    }

    @Override
    public void wrenchUsed(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        player.swing(hand);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            // Rotation happens on the server, which syncs the new state back
            return InteractionResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        InteractionResult result =
            CustomRotationHelper.INSTANCE.attemptRotateBlock(level, pos, state, context.getClickedFace());
        if (result == InteractionResult.PASS) {
            result = VanillaRotationHandlers.rotateGeneric(level, pos, state);
        }
        if (result == InteractionResult.SUCCESS) {
            Player player = context.getPlayer();
            if (player != null) {
                BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos,
                    context.isInside());
                wrenchUsed(player, context.getHand(), context.getItemInHand(), hit);
            }
            SoundType sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2,
                sound.getPitch() * 0.8f);
        }
        return result == InteractionResult.SUCCESS ? InteractionResult.CONSUME : result;
    }
}
