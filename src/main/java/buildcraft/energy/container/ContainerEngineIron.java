/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;

import buildcraft.lib.gui.ContainerBCTile;

import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.tile.TileEngineIron;

public class ContainerEngineIron extends ContainerBCTile<TileEngineIron> {
    public ContainerEngineIron(int id, Player player, TileEngineIron engine) {
        super(BCEnergyMenus.ENGINE_IRON.get(), id, player, engine);
        addFullPlayerInventory(95);
    }

    /** The client-side constructor. */
    public ContainerEngineIron(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileEngineIron.class));
    }

    /** The only slots are the player's: shift-clicking a fluid container empties it into the engine (or fills it with
     * residue). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        FluidActionResult result = FluidUtil.tryEmptyContainer(
            stack, tile.getFluidHandler(), Integer.MAX_VALUE, player, true
        );
        if (!result.isSuccess()) {
            result = FluidUtil.tryFillContainer(stack, tile.getFluidHandler(), Integer.MAX_VALUE, player, true);
        }
        if (result.isSuccess()) {
            ItemStack remaining = stack.copy();
            remaining.shrink(1);
            if (remaining.isEmpty()) {
                slot.set(result.getResult());
            } else {
                slot.set(remaining);
                if (!player.getInventory().add(result.getResult())) {
                    player.drop(result.getResult(), false);
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
