/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.fml.LogicalSide;

/** Something (usually a block entity) that can receive a {@link MessageUpdateTile}. */
public interface IPayloadReceiver {
    /** @param side The side that received the message.
     * @param sender The player that sent the message, or null if it was sent by the server. */
    void receivePayload(LogicalSide side, @Nullable ServerPlayer sender, FriendlyByteBuf buffer);
}
