/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.items.SlotItemHandler;

import buildcraft.lib.gui.ContainerBCTile;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.tile.TileChute;

public class ContainerChute extends ContainerBCTile<TileChute> {
    public ContainerChute(int id, Player player, TileChute chute) {
        super(BCFactoryMenus.CHUTE.get(), id, player, chute);

        addFullPlayerInventory(71);
        addSlot(new SlotItemHandler(chute.inv, 0, 62, 18));
        addSlot(new SlotItemHandler(chute.inv, 1, 80, 18));
        addSlot(new SlotItemHandler(chute.inv, 2, 98, 18));
        addSlot(new SlotItemHandler(chute.inv, 3, 80, 36));
    }

    /** The client-side constructor. */
    public ContainerChute(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileChute.class));
    }
}
