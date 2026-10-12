/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.tile.TileBC;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;
import buildcraft.transport.tile.TilePipeHolder;

/** The 6 rows of 9 filters of a diamond pipe (one row per side), as phantom slots. */
public class ContainerDiamondPipe extends ContainerBCTile<TileBC> {
    public ContainerDiamondPipe(int id, Player player, TileBC tile) {
        super(BCTransportMenus.DIAMOND_PIPE.get(), id, player, tile);
        if (!(tile instanceof TilePipeHolder holder)
            || !(holder.getPipe().getBehaviour() instanceof PipeBehaviourDiamond diamond)) {
            throw new IllegalStateException("Not a diamond pipe: " + tile);
        }
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < PipeBehaviourDiamond.FILTERS_PER_SIDE; x++) {
                addSlot(new SlotPhantom(diamond.filters, x + y * 9, 8 + x * 18, 18 + y * 18));
            }
        }
        addFullPlayerInventory(140);
    }

    /** The client-side constructor. */
    public ContainerDiamondPipe(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TilePipeHolder.class));
    }
}
