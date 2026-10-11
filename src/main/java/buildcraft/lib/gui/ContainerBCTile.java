/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.tile.TileBC;

/** A menu attached to a {@link TileBC}. */
public abstract class ContainerBCTile<T extends TileBC> extends ContainerBC {
    public final T tile;

    public ContainerBCTile(@Nullable MenuType<?> type, int containerId, Player player, T tile) {
        super(type, containerId, player);
        this.tile = tile;
        if (!player.level().isClientSide) {
            tile.onPlayerOpen(player);
        }
    }

    /** Reads the tile written by {@link TileBC#openMenu}, for the client-side menu factory.
     *
     * @throws IllegalStateException if the tile is missing or of the wrong type (the server and client disagree) */
    public static <T extends TileBC> T readTile(Inventory inventory, FriendlyByteBuf buffer, Class<T> tileClass) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity tile = inventory.player.level().getBlockEntity(pos);
        if (!tileClass.isInstance(tile)) {
            throw new IllegalStateException(
                "Expected a " + tileClass.getSimpleName() + " at " + pos + ", found " + tile
            );
        }
        return tileClass.cast(tile);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        tile.onPlayerClose(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return tile.canInteractWith(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        tile.sendNetworkGuiTick(this.player);
    }
}
