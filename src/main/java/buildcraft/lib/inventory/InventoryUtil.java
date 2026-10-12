/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.api.transport.IInjectable;

public final class InventoryUtil {
    private InventoryUtil() {}

    /** Gives the stack to a neighbouring pipe if there is one, otherwise to a neighbouring inventory, and drops what is
     * left above the block. */
    public static void addToBestAcceptor(Level level, BlockPos pos, @Nonnull ItemStack stack) {
        stack = addToRandomInjectable(level, pos, stack);
        stack = addToRandomInventory(level, pos, stack);
        if (!stack.isEmpty()) {
            Block.popResource(level, pos.above(), stack);
        }
    }

    @Nonnull
    public static ItemStack addToRandomInjectable(Level level, BlockPos pos, @Nonnull ItemStack stack) {
        for (Direction side : shuffledDirections(level)) {
            if (stack.isEmpty()) {
                break;
            }
            IInjectable injectable = ItemTransactorHelper.getInjectable(
                level.getBlockEntity(pos.relative(side)), side.getOpposite()
            );
            if (injectable != null && injectable.canInjectItems(side.getOpposite())) {
                stack = injectable.injectItem(stack, true, side.getOpposite(), null, 0);
            }
        }
        return stack;
    }

    @Nonnull
    public static ItemStack addToRandomInventory(Level level, BlockPos pos, @Nonnull ItemStack stack) {
        for (Direction side : shuffledDirections(level)) {
            if (stack.isEmpty()) {
                break;
            }
            IItemHandler handler = ItemTransactorHelper.getItemHandler(
                level.getBlockEntity(pos.relative(side)), side.getOpposite()
            );
            if (handler != null) {
                stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            }
        }
        return stack;
    }

    private static List<Direction> shuffledDirections(Level level) {
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(sides, new Random(level.random.nextLong()));
        return sides;
    }
}
