/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import buildcraft.lib.gui.ContainerBC;

/** Sends a message to the {@link ContainerBC} that a player currently has open, in either direction. */
public final class MessageContainer {
    public final int windowId;
    public final int msgId;
    private final byte[] payload;

    public MessageContainer(int windowId, int msgId, FriendlyByteBuf payload) {
        this.windowId = windowId;
        this.msgId = msgId;
        this.payload = new byte[payload.readableBytes()];
        payload.readBytes(this.payload);
    }

    private MessageContainer(int windowId, int msgId, byte[] payload) {
        this.windowId = windowId;
        this.msgId = msgId;
        this.payload = payload;
    }

    public FriendlyByteBuf getPayload() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
    }

    public static void encode(MessageContainer message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.windowId);
        buffer.writeVarInt(message.msgId);
        buffer.writeByteArray(message.payload);
    }

    public static MessageContainer decode(FriendlyByteBuf buffer) {
        int windowId = buffer.readVarInt();
        int msgId = buffer.readVarInt();
        byte[] payload = buffer.readByteArray();
        return new MessageContainer(windowId, msgId, payload);
    }

    public static void handle(MessageContainer message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender == null) {
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientMessageHandler.handleContainer(message);
            }));
        } else {
            ctx.enqueueWork(() -> dispatch(sender, message, LogicalSide.SERVER));
        }
        ctx.setPacketHandled(true);
    }

    static void dispatch(Player player, MessageContainer message, LogicalSide side) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof ContainerBC container && menu.containerId == message.windowId) {
            container.readMessage(message.msgId, message.getPayload(), side, side.isServer() ? (ServerPlayer) player
                : null);
        }
    }
}
