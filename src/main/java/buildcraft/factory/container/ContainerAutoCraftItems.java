/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import net.minecraftforge.items.SlotItemHandler;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.gui.slot.SlotPhantom;

import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.tile.TileAutoWorkbenchItems;

public class ContainerAutoCraftItems extends ContainerBCTile<TileAutoWorkbenchItems> {
    public final Slot[] materialSlots = new Slot[9];

    public ContainerAutoCraftItems(int id, Player player, TileAutoWorkbenchItems tile) {
        super(BCFactoryMenus.AUTO_WORKBENCH_ITEMS.get(), id, player, tile);

        addSlot(new SlotOutput(tile.invResult, 0, 124, 35));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(tile.invBlueprint, x + y * 3, 30 + x * 18, 17 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            materialSlots[x] = addSlot(new SlotItemHandler(tile.invMaterials, x, 8 + x * 18, 84));
        }
        addFullPlayerInventory(115);
    }

    /** The client-side constructor. */
    public ContainerAutoCraftItems(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileAutoWorkbenchItems.class));
    }
}
