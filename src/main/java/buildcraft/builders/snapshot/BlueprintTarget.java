/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.IItemHandler;

import buildcraft.lib.misc.BlockUtil;

/** Builds a blueprint: every block exactly as it was scanned (turned with the builder). */
public class BlueprintTarget extends BuildTarget {
    private final Blueprint blueprint;
    private final List<BlockState> palette = new ArrayList<>();
    private final List<ItemStack> paletteItems = new ArrayList<>();

    public BlueprintTarget(Blueprint blueprint, BlockPos origin, Rotation rotation) {
        super(blueprint.size, origin, rotation);
        this.blueprint = blueprint;
        for (BlockState state : blueprint.palette) {
            BlockState rotated = state.rotate(rotation);
            palette.add(rotated);
            paletteItems.add(Blueprint.getRequiredItem(rotated));
        }
    }

    private BlockState wanted(int index) {
        return palette.get(blueprint.data[index]);
    }

    @Override
    public Result check(Level level, int index, BlockPos pos) {
        BlockState wanted = wanted(index);
        BlockState current = level.getBlockState(pos);
        if (current == wanted) {
            return Result.CORRECT;
        }
        boolean empty = current.isAir() || current.getBlock() instanceof LiquidBlock;
        if (wanted.isAir()) {
            return empty || !BlockUtil.canMachineBreak(level, pos) ? Result.CORRECT : Result.TO_BREAK;
        }
        if (empty) {
            return Result.TO_PLACE;
        }
        // Another block (or the same one, the wrong way) is in the way
        return BlockUtil.canMachineBreak(level, pos) ? Result.TO_BREAK : Result.CORRECT;
    }

    @Override
    public ItemStack takeItems(IItemHandler inventory, int index) {
        ItemStack required = paletteItems.get(blueprint.data[index]);
        if (required.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // Check that there are enough first
        int found = 0;
        for (int slot = 0; slot < inventory.getSlots() && found < required.getCount(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItem(stack, required)) {
                found += stack.getCount();
            }
        }
        if (found < required.getCount()) {
            return null;
        }
        int left = required.getCount();
        for (int slot = 0; slot < inventory.getSlots() && left > 0; slot++) {
            if (ItemStack.isSameItem(inventory.getStackInSlot(slot), required)) {
                left -= inventory.extractItem(slot, left, false).getCount();
            }
        }
        return required.copy();
    }

    @Override
    public List<ItemStack> getRequiredItems(int index) {
        ItemStack required = paletteItems.get(blueprint.data[index]);
        return required.isEmpty() ? List.of() : List.of(required);
    }

    @Override
    public boolean place(ServerLevel level, int index, BlockPos pos, ItemStack items, GameProfile owner) {
        BlockState state = wanted(index);
        if (!state.canSurvive(level, pos) && !state.isAir()) {
            // Like a torch without its wall yet: try again later
            return false;
        }
        // Neighbours aren't told, so that half-built things (like doors) don't fall apart
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
