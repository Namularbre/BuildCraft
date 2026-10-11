/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import net.minecraftforge.fml.LogicalSide;

/** Client-only message handling. Only referenced through {@link net.minecraftforge.fml.DistExecutor}, so that this
 * class is never loaded on a dedicated server. */
final class ClientMessageHandler {
    private ClientMessageHandler() {}

    static void handleUpdateTile(MessageUpdateTile message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.isLoaded(message.pos)) {
            MessageUpdateTile.dispatch(level, message);
        }
    }

    static void handleContainer(MessageContainer message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            MessageContainer.dispatch(player, message, LogicalSide.CLIENT);
        }
    }
}
