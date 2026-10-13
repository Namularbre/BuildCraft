/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.container;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;
import buildcraft.transport.tile.TilePipeHolder;

/** The 9 filters of a diamond wooden pipe, and its filter mode. */
public class ContainerDiamondWoodPipe extends ContainerBCTile<TileBC> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("diamond_wood_pipe");
    public static final int ID_FILTER_MODE = IDS.allocId("FILTER_MODE");

    public final PipeBehaviourWoodDiamond behaviour;

    public ContainerDiamondWoodPipe(int id, Player player, TileBC tile) {
        super(BCTransportMenus.DIAMOND_WOOD_PIPE.get(), id, player, tile);
        if (!(tile instanceof TilePipeHolder holder)
            || !(holder.getPipe().getBehaviour() instanceof PipeBehaviourWoodDiamond diamond)) {
            throw new IllegalStateException("Not a diamond wooden pipe: " + tile);
        }
        behaviour = diamond;
        for (int i = 0; i < PipeBehaviourWoodDiamond.FILTER_COUNT; i++) {
            addSlot(new SlotPhantom(behaviour.filters, i, 8 + i * 18, 18));
        }
        addFullPlayerInventory(79);
    }

    /** The client-side constructor. */
    public ContainerDiamondWoodPipe(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TilePipeHolder.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public void setFilterMode(FilterMode mode) {
        behaviour.filterMode = mode;
        sendMessage(ID_FILTER_MODE, buffer -> buffer.writeEnum(mode));
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        if (side == LogicalSide.SERVER && id == ID_FILTER_MODE) {
            behaviour.setFilterMode(buffer.readEnum(FilterMode.class));
        }
    }
}
