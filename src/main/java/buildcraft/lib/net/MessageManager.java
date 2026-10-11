/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import buildcraft.BuildCraft;

/** Owns BuildCraft's network channel, and provides helpers to send messages over it. */
public final class MessageManager {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "main"), () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    private MessageManager() {}

    public static void registerMessages() {
        int id = 0;
        CHANNEL.registerMessage(
            id++, MessageUpdateTile.class, MessageUpdateTile::encode, MessageUpdateTile::decode,
            MessageUpdateTile::handle
        );
        CHANNEL.registerMessage(
            id++, MessageContainer.class, MessageContainer::encode, MessageContainer::decode,
            MessageContainer::handle
        );
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendTo(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToPlayers(Collection<? extends Player> players, Object message) {
        for (Player player : players) {
            if (player instanceof ServerPlayer serverPlayer) {
                sendTo(message, serverPlayer);
            }
        }
    }

    /** Sends the message to every player that has the chunk containing the given position loaded. */
    public static void sendToAllWatching(Level level, BlockPos pos, Object message) {
        if (level.isClientSide || !level.isLoaded(pos)) {
            return;
        }
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)), message);
    }
}
