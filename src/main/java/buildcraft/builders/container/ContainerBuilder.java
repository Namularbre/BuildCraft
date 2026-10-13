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
import buildcraft.lib.misc.data.IdAllocator;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.tile.TileBuilder;

public class ContainerBuilder extends ContainerBCTile<TileBuilder> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("builder");
    public static final int ID_EXCAVATE = IDS.allocId("EXCAVATE");

    public ContainerBuilder(int id, Player player, TileBuilder tile) {
        super(BCBuildersMenus.BUILDER.get(), id, player, tile);
        addFullPlayerInventory(140);
        addSlot(new SlotItemHandler(tile.invSnapshot, 0, 80, 27));
        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlot(new SlotItemHandler(tile.invResources, sx + sy * 9, 8 + sx * 18, 72 + sy * 18));
            }
        }
    }

    /** The client-side constructor. */
    public ContainerBuilder(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileBuilder.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public void setExcavate(boolean value) {
        tile.setCanExcavate(value);
        sendMessage(ID_EXCAVATE, buffer -> buffer.writeBoolean(value));
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        if (side == LogicalSide.SERVER && id == ID_EXCAVATE) {
            tile.setCanExcavate(buffer.readBoolean());
        }
    }
}
