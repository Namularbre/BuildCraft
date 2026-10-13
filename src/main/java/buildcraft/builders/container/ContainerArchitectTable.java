/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.SlotItemHandler;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.misc.data.IdAllocator;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.tile.TileArchitectTable;

public class ContainerArchitectTable extends ContainerBCTile<TileArchitectTable> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("architect_table");
    public static final int ID_NAME = IDS.allocId("NAME");
    /** Names longer than this are cut. */
    public static final int MAX_NAME_LENGTH = 64;

    public ContainerArchitectTable(int id, Player player, TileArchitectTable tile) {
        super(BCBuildersMenus.ARCHITECT_TABLE.get(), id, player, tile);
        addFullPlayerInventory(88, 84);
        addSlot(new SlotItemHandler(tile.invIn, 0, 135, 35));
        addSlot(new SlotOutput(tile.invOut, 0, 194, 35));
    }

    /** The client-side constructor. */
    public ContainerArchitectTable(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileArchitectTable.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public void sendName(String name) {
        sendMessage(ID_NAME, buffer -> buffer.writeUtf(name, MAX_NAME_LENGTH));
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        if (side == LogicalSide.SERVER && id == ID_NAME) {
            tile.name = buffer.readUtf(MAX_NAME_LENGTH).trim();
            tile.setChanged();
            tile.sendNetworkUpdate(TileArchitectTable.NET_RENDER_DATA);
        }
    }
}
