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
import net.minecraft.world.item.DyeColor;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.TileBC;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;
import buildcraft.transport.tile.TilePipeHolder;

/** The 4 filters of an emzuli pipe, and the colours that they paint the items with. */
public class ContainerEmzuliPipe extends ContainerBCTile<TileBC> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("emzuli_pipe");
    public static final int ID_PAINT = IDS.allocId("PAINT");
    /** Where the filter of each {@link SlotIndex} is. */
    public static final int[][] SLOT_POSITIONS = { { 25, 21 }, { 25, 49 }, { 134, 21 }, { 134, 49 } };

    public final PipeBehaviourEmzuli behaviour;

    public ContainerEmzuliPipe(int id, Player player, TileBC tile) {
        super(BCTransportMenus.EMZULI_PIPE.get(), id, player, tile);
        if (!(tile instanceof TilePipeHolder holder)
            || !(holder.getPipe().getBehaviour() instanceof PipeBehaviourEmzuli emzuli)) {
            throw new IllegalStateException("Not an emzuli pipe: " + tile);
        }
        behaviour = emzuli;
        for (SlotIndex index : SlotIndex.VALUES) {
            int[] pos = SLOT_POSITIONS[index.ordinal()];
            addSlot(new SlotPhantom(behaviour.invFilters, index.ordinal(), pos[0], pos[1]));
        }
        addFullPlayerInventory(84);
    }

    /** The client-side constructor. */
    public ContainerEmzuliPipe(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TilePipeHolder.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    public void setPaint(SlotIndex index, @Nullable DyeColor colour) {
        sendMessage(ID_PAINT, buffer -> {
            buffer.writeEnum(index);
            buffer.writeByte(colour == null ? -1 : colour.getId());
        });
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        if (side == LogicalSide.SERVER && id == ID_PAINT) {
            SlotIndex index = buffer.readEnum(SlotIndex.class);
            int colour = buffer.readByte();
            behaviour.setSlotColour(index, colour < 0 ? null : DyeColor.byId(colour & 15));
        }
    }
}
