/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.tile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkHooks;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.IPlayerOwned;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IItemPipe;

import buildcraft.lib.cap.CapabilityHelper;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.IPayloadReceiver;
import buildcraft.lib.net.IPayloadWriter;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageUpdateTile;

/** Base class for every BuildCraft block entity. Handles ownership, capabilities, saving, and network syncing through
 * id-tagged payloads. */
public abstract class TileBC extends BlockEntity implements IPayloadReceiver, IPlayerOwned {
    /** Increment whenever the saved format changes, and handle the old format in {@link #migrateOldNBT(int,
     * CompoundTag)}. */
    public static final int DATA_VERSION = 1;

    /** The {@link GameProfile} used when a tile cannot determine its real owner. */
    public static final GameProfile NULL_PROFILE = new GameProfile(
        UUID.nameUUIDFromBytes("buildcraft.core".getBytes(StandardCharsets.UTF_8)), "[BuildCraft]"
    );

    protected static final IdAllocator IDS = new IdAllocator("tile");

    /** Used for sending all data used for rendering the tile on a client. This does not include items, power, stages,
     * etc (Unless some are shown in the world) */
    public static final int NET_RENDER_DATA = IDS.allocId("RENDER_DATA");
    /** Used for sending all data in the GUI. Basically what has been omitted from {@link #NET_RENDER_DATA} that is
     * shown in the GUI. */
    public static final int NET_GUI_DATA = IDS.allocId("GUI_DATA");
    /** Used for sending the data that would normally be sent with
     * {@link net.minecraft.world.inventory.AbstractContainerMenu#broadcastChanges()}. Note that if no bytes are written
     * then the update message won't be sent. You should detect if any changes have been made to the gui since the last
     * tick, so you don't resend duplicate information if nothing has changed by the next tick. */
    public static final int NET_GUI_TICK = IDS.allocId("GUI_TICK");
    /** Used to tell the client to redraw the block. */
    public static final int NET_REDRAW = IDS.allocId("REDRAW");

    protected final CapabilityHelper caps = new CapabilityHelper();

    /** Handles all of the players that are currently using this tile (have a GUI open) */
    private final Set<Player> usingPlayers = Sets.newIdentityHashSet();
    private GameProfile owner;

    public TileBC(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ##################################################
    //
    // Local blockstate + block entity getters
    //
    // ##################################################

    public final BlockState getNeighbourState(Direction offset) {
        return getLocalState(worldPosition.relative(offset));
    }

    /** @param pos The <i>absolute</i> position of the {@link BlockState}. */
    public final BlockState getLocalState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    /** @return The neighbouring block entity, or null if there is none or if it isn't loaded. */
    @Nullable
    public final BlockEntity getNeighbourTile(Direction offset) {
        return getLocalTile(worldPosition.relative(offset));
    }

    /** @param pos The <i>absolute</i> position of the {@link BlockEntity}.
     * @return The block entity, or null if there is none or if it isn't loaded. */
    @Nullable
    public final BlockEntity getLocalTile(BlockPos pos) {
        if (level == null || !level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos);
    }

    // ##################
    //
    // Misc overridables
    //
    // ##################

    /** @return The {@link IdAllocator} that allocates all ID's for this class, and its parent classes. All subclasses
     *         should override this if they allocate their own ids after calling
     *         {@link IdAllocator#makeChild(String)} */
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    /** Called whenever the block holding this tile is exploded. */
    public void onExplode(Explosion explosion) {}

    /** Called whenever the block is removed (broken, exploded, or replaced by a different block). */
    public void onRemove() {
        for (ItemStack stack : collectDrops()) {
            Block.popResource(level, worldPosition, stack);
        }
    }

    /** @return Every stack that should be dropped when this block is removed, in addition to the block itself. */
    protected Iterable<ItemStack> collectDrops() {
        return Collections.emptyList();
    }

    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (placer instanceof Player player) {
            owner = player.getGameProfile();
        } else {
            BCLog.logger.warn("[lib.tile] {} was placed by a non-player ({}), it has no owner", this, placer);
        }
    }

    public void onPlayerOpen(Player player) {
        if (owner == null || owner == NULL_PROFILE) {
            owner = player.getGameProfile();
        }
        sendNetworkUpdate(NET_GUI_DATA, player);
        usingPlayers.add(player);
    }

    public void onPlayerClose(Player player) {
        usingPlayers.remove(player);
    }

    /** Block interactions happen before item ones, so blocks with a GUI must let wrenches (to rotate the block) and
     * pipes (to place them against the block) through.
     *
     * @return True if the player is holding something that should be used on the block rather than open its GUI. */
    protected static boolean isHoldingTool(Player player, InteractionHand hand) {
        Item item = player.getItemInHand(hand).getItem();
        return item instanceof IToolWrench || item instanceof IItemPipe;
    }

    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public void onNeighbourBlockChanged(Block block, BlockPos neighbour) {}

    /** Opens a menu for the given player. The menu's client-side factory reads this tile's position from the buffer
     * with {@link buildcraft.lib.gui.ContainerBCTile#readTile}. */
    public void openMenu(ServerPlayer player, Component title, MenuConstructor menu) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(menu, title), worldPosition);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        LazyOptional<T> optional = caps.getCapability(cap, side);
        if (optional.isPresent()) {
            return optional;
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        caps.invalidate();
    }

    /** Cheaper version of {@link #setChanged()} that doesn't update nearby comparators, so all it will do is ensure
     * that the current chunk is saved after the last tick. */
    public void markChunkDirty() {
        if (level != null) {
            level.blockEntityChanged(worldPosition);
        }
    }

    // ##################
    //
    // Permission related
    //
    // ##################

    protected boolean hasOwner() {
        return owner != null;
    }

    @Override
    public GameProfile getOwner() {
        if (owner == null) {
            BCLog.logger.warn("[lib.tile] Unknown owner for {} at {}", getClass(), worldPosition);
            owner = NULL_PROFILE;
        }
        return owner;
    }

    public boolean canInteractWith(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    // ##################
    //
    // Network helpers
    //
    // ##################

    /** Tells MC to redraw this block. On the server this sends the {@link #NET_REDRAW} message. */
    public final void redrawBlock() {
        if (level != null) {
            if (level.isClientSide) {
                // Dynamic models read their state from the model data
                requestModelDataUpdate();
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
            } else {
                sendNetworkUpdate(NET_REDRAW);
            }
        }
    }

    /** Sends a network update update of the specified ID. */
    public final void sendNetworkUpdate(int id) {
        if (level != null) {
            MessageUpdateTile message = createNetworkUpdate(id);
            if (level.isClientSide) {
                MessageManager.sendToServer(message);
            } else {
                MessageManager.sendToAllWatching(level, worldPosition, message);
            }
        }
    }

    public final void sendNetworkGuiTick(Player player) {
        if (level != null && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MessageUpdateTile message = createNetworkUpdate(NET_GUI_TICK);
            if (message.getPayloadSize() <= Short.BYTES) {
                return;
            }
            MessageManager.sendTo(message, serverPlayer);
        }
    }

    public final void sendNetworkGuiUpdate(int id) {
        if (level != null) {
            for (Player player : usingPlayers) {
                sendNetworkUpdate(id, player);
            }
        }
    }

    public final void sendNetworkUpdate(int id, Player target) {
        if (level != null && target instanceof ServerPlayer serverPlayer) {
            MessageManager.sendTo(createNetworkUpdate(id), serverPlayer);
        }
    }

    public final MessageUpdateTile createNetworkUpdate(final int id) {
        LogicalSide side = getSide();
        return createMessage(id, (buffer) -> writePayload(id, buffer, side));
    }

    public final void createAndSendMessage(int id, IPayloadWriter writer) {
        if (level != null) {
            MessageUpdateTile message = createMessage(id, writer);
            if (level.isClientSide) {
                MessageManager.sendToServer(message);
            } else {
                MessageManager.sendToAllWatching(level, worldPosition, message);
            }
        }
    }

    public final void createAndSendGuiMessage(int id, IPayloadWriter writer) {
        if (level != null) {
            MessageUpdateTile message = createMessage(id, writer);
            if (level.isClientSide) {
                MessageManager.sendToServer(message);
            } else {
                MessageManager.sendToPlayers(usingPlayers, message);
            }
        }
    }

    public final void createAndSendMessage(int id, ServerPlayer player, IPayloadWriter writer) {
        MessageManager.sendTo(createMessage(id, writer), player);
    }

    public final MessageUpdateTile createMessage(int id, IPayloadWriter writer) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(id);
        writer.write(buffer);
        return new MessageUpdateTile(worldPosition, buffer);
    }

    private LogicalSide getSide() {
        return level != null && level.isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(NET_RENDER_DATA);
        writePayload(NET_RENDER_DATA, buffer, getSide());
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);

        CompoundTag nbt = new CompoundTag();
        nbt.putByteArray("d", bytes);
        return nbt;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        // Explicitly don't read the (server) save data from the tag: only the render payload is sent
        if (!tag.contains("d", Tag.TAG_BYTE_ARRAY)) {
            // A bit odd, but ok - this was probably sent by something else
            return;
        }
        byte[] bytes = tag.getByteArray("d");
        if (bytes.length < 2) {
            // less than 2 bytes won't even be enough to read the ID, so we'll treat it as no data.
            BCLog.logger.warn("[lib.tile] Received an update tag that didn't have any data!\n\t(" + tag + ")");
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        int id = buffer.readUnsignedShort();
        readPayload(id, buffer, getSide(), null);
        ensureEmpty(buffer, id);
    }

    @Override
    public final void receivePayload(LogicalSide side, @Nullable ServerPlayer sender, FriendlyByteBuf buffer) {
        int id = buffer.readUnsignedShort();
        readPayload(id, buffer, side, sender);
        ensureEmpty(buffer, id);
    }

    /** Makes sure that we actually read the entire message rather than just discarding it. */
    private void ensureEmpty(FriendlyByteBuf buffer, int id) {
        int readable = buffer.readableBytes();
        if (readable > 0) {
            BCLog.logger.warn(
                "[lib.tile] Didn't read all {} bytes of {} (id = {}) on the {}", readable, getClass(), getIdAllocator()
                    .getNameFor(id), getSide()
            );
        }
    }

    // ######################
    //
    // Network overridables
    //
    // ######################

    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        // write render data with gui data
        if (id == NET_GUI_DATA) {
            writePayload(NET_RENDER_DATA, buffer, side);

            if (side == LogicalSide.SERVER) {
                writeGameProfile(buffer, owner);
            }
        }
    }

    /** @param sender The player that sent this payload, or null if it was sent by the server (or is an update tag). */
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        // read render data with gui data
        if (id == NET_GUI_DATA) {
            readPayload(NET_RENDER_DATA, buffer, side, sender);

            if (side == LogicalSide.CLIENT) {
                owner = readGameProfile(buffer);
            }
        }
        if (side == LogicalSide.CLIENT) {
            if (id == NET_REDRAW) {
                redrawBlock();
            }
        }
    }

    private static void writeGameProfile(FriendlyByteBuf buffer, @Nullable GameProfile profile) {
        if (profile != null && profile.getId() != null && profile.getName() != null) {
            buffer.writeBoolean(true);
            buffer.writeUUID(profile.getId());
            buffer.writeUtf(profile.getName());
        } else {
            buffer.writeBoolean(false);
        }
    }

    @Nullable
    private static GameProfile readGameProfile(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf();
            return new GameProfile(id, name);
        }
        return null;
    }

    // ######################
    //
    // NBT handling
    //
    // ######################

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        migrateOldNBT(nbt.getInt("data-version"), nbt);
        if (nbt.contains("owner", Tag.TAG_COMPOUND)) {
            owner = NbtUtils.readGameProfile(nbt.getCompound("owner"));
        }
    }

    /** Called before anything else is read in {@link #load(CompoundTag)}.
     *
     * @param version The {@link #DATA_VERSION} that the data was saved with, or 0 if it didn't have one. */
    protected void migrateOldNBT(int version, CompoundTag nbt) {}

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("data-version", DATA_VERSION);
        if (owner != null && owner.isComplete() && owner != NULL_PROFILE) {
            nbt.put("owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        }
    }
}
