/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import buildcraft.api.core.BCLog;

/** Sends an arbitrary payload to the {@link IPayloadReceiver} block entity at a given position, in either direction. */
public final class MessageUpdateTile {
    /** Players can only send updates to blocks within this distance (squared) of them. */
    private static final double MAX_DIST_SQ = 64 * 64;

    public final BlockPos pos;
    private final byte[] payload;

    public MessageUpdateTile(BlockPos pos, FriendlyByteBuf payload) {
        this.pos = pos;
        this.payload = new byte[payload.readableBytes()];
        payload.readBytes(this.payload);
    }

    private MessageUpdateTile(BlockPos pos, byte[] payload) {
        this.pos = pos;
        this.payload = payload;
    }

    public int getPayloadSize() {
        return payload.length;
    }

    public FriendlyByteBuf getPayload() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
    }

    public static void encode(MessageUpdateTile message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByteArray(message.payload);
    }

    public static MessageUpdateTile decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        byte[] payload = buffer.readByteArray();
        return new MessageUpdateTile(pos, payload);
    }

    public static void handle(MessageUpdateTile message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender == null) {
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientMessageHandler.handleUpdateTile(message);
            }));
        } else {
            ctx.enqueueWork(() -> handleServer(message, sender));
        }
        ctx.setPacketHandled(true);
    }

    private static void handleServer(MessageUpdateTile message, ServerPlayer sender) {
        Level level = sender.level();
        if (!level.isLoaded(message.pos)) {
            return;
        }
        if (sender.distanceToSqr(message.pos.getCenter()) > MAX_DIST_SQ) {
            BCLog.logger.warn("[lib.net] {} sent a tile update for {}, which is too far away", sender.getName()
                .getString(), message.pos);
            return;
        }
        BlockEntity tile = level.getBlockEntity(message.pos);
        if (tile instanceof IPayloadReceiver receiver) {
            receiver.receivePayload(LogicalSide.SERVER, sender, message.getPayload());
        }
    }

    static void dispatch(Level level, MessageUpdateTile message) {
        BlockEntity tile = level.getBlockEntity(message.pos);
        if (tile instanceof IPayloadReceiver receiver) {
            receiver.receivePayload(LogicalSide.CLIENT, null, message.getPayload());
        }
    }
}
