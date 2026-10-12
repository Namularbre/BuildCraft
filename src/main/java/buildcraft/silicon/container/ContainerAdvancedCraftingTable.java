/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.items.SlotItemHandler;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.gui.slot.SlotPhantom;

import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;

public class ContainerAdvancedCraftingTable extends ContainerBCTile<TileAdvancedCraftingTable> {
    public ContainerAdvancedCraftingTable(int id, Player player, TileAdvancedCraftingTable tile) {
        super(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), id, player, tile);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotPhantom(tile.invBlueprint, x + y * 3, 33 + x * 18, 16 + y * 18));
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                addSlot(new SlotItemHandler(tile.invMaterials, x + y * 5, 15 + x * 18, 85 + y * 18));
            }
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new SlotOutput(tile.invResults, x + y * 3, 109 + x * 18, 85 + y * 18));
            }
        }
        addFullPlayerInventory(153);
    }

    /** The client-side constructor. */
    public ContainerAdvancedCraftingTable(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileAdvancedCraftingTable.class));
    }
}
