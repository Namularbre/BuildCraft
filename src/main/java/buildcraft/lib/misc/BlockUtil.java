/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.List;
import java.util.Optional;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;

public final class BlockUtil {
    private BlockUtil() {}

    /** Breaks the block like the given player would with a diamond pickaxe, if no other mod (like a protection mod)
     * stops it.
     *
     * @return What the block dropped, or nothing if it couldn't be broken. */
    public static Optional<List<ItemStack>> breakBlockAndGetDrops(ServerLevel level, BlockPos pos, GameProfile owner) {
        BlockState state = level.getBlockState(pos);
        FakePlayer fakePlayer = FakePlayerFactory.get(level, owner);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, fakePlayer))) {
            return Optional.empty();
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), fakePlayer, tool);
        level.destroyBlock(pos, false, fakePlayer);
        return Optional.of(drops);
    }

    /** @return True if machines can break the block: it isn't air or unbreakable, and isn't a fluid thicker than
     *         water (like lava). */
    public static boolean canMachineBreak(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return state.getFluidState().getFluidType().getViscosity() <= 1000;
        }
        return true;
    }
}
