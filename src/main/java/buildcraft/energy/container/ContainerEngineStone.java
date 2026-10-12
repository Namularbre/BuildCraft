/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.items.SlotItemHandler;

import buildcraft.lib.gui.ContainerBCTile;

import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.tile.TileEngineStone;

public class ContainerEngineStone extends ContainerBCTile<TileEngineStone> {
    public ContainerEngineStone(int id, Player player, TileEngineStone engine) {
        super(BCEnergyMenus.ENGINE_STONE.get(), id, player, engine);

        addFullPlayerInventory(84);
        addSlot(new SlotItemHandler(engine.invFuel, 0, 80, 41));
        addDataSlots(engine.data);
    }

    /** The client-side constructor. */
    public ContainerEngineStone(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileEngineStone.class));
    }

    /** @return How much of the current fuel item is left to burn, from 0 to 1. */
    public double getFuelLeft() {
        int total = tile.data.get(1);
        return total <= 0 ? 0 : tile.data.get(0) / (double) total;
    }
}
