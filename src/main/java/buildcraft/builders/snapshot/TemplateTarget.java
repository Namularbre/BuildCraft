/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.BitSet;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandler;

import buildcraft.lib.misc.BlockUtil;

/** Builds a template: any block goes where the template is set, and nothing goes elsewhere. */
public class TemplateTarget extends BuildTarget {
    private final BitSet data;

    public TemplateTarget(BitSet data, BlockPos size, BlockPos origin, Rotation rotation) {
        super(size, origin, rotation);
        this.data = data;
    }

    @Override
    public Result check(Level level, int index, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean empty = state.isAir() || state.getBlock() instanceof LiquidBlock;
        if (data.get(index)) {
            // Any block is fine, as long as there is one (fluids get replaced)
            return empty ? Result.TO_PLACE : Result.CORRECT;
        }
        return empty || !BlockUtil.canMachineBreak(level, pos) ? Result.CORRECT : Result.TO_BREAK;
    }

    @Override
    public ItemStack takeItems(IItemHandler inventory, int index) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof BlockItem) {
                ItemStack stack = inventory.extractItem(slot, 1, false);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
        }
        return null;
    }

    @Override
    public List<ItemStack> getRequiredItems(int index) {
        return List.of();
    }

    /** Places the block like the owner would (so it is turned the way blocks usually are when placed). */
    @Override
    public boolean place(ServerLevel level, int index, BlockPos pos, ItemStack items, GameProfile owner) {
        if (!(items.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        FakePlayer player = FakePlayerFactory.get(level, owner);
        ItemStack stack = items.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        boolean placed = blockItem.place(context).consumesAction();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return placed;
    }
}
