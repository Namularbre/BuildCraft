/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.tile;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.PipeEventBus;
import buildcraft.transport.pipe.PipeModelKey;

/** The block entity of every pipe. Holds the {@link Pipe} itself, and connects it to the world. */
public class TilePipeHolder extends TileBC implements IPipeHolder, ITickable {
    /** The model of the pipe, for its dynamic block model. */
    public static final ModelProperty<PipeModelKey> MODEL_KEY = new ModelProperty<>();
    /** The models of the pluggables on each side (null where there isn't one, or it isn't drawn in the model). */
    public static final ModelProperty<PluggableModelKey[]> PLUG_KEYS = new ModelProperty<>();
    // What pluggable messages hold
    private static final int PLUG_REMOVE = 0, PLUG_CREATE = 1, PLUG_UPDATE = 2;

    protected static final IdAllocator IDS = TileBC.IDS.makeChild("pipe");

    public static final int NET_UPDATE_MULTI = IDS.allocId("UPDATE_MULTI");
    public static final int NET_UPDATE_PIPE_BEHAVIOUR = getReceiverId(PipeMessageReceiver.BEHAVIOUR);
    public static final int NET_UPDATE_PIPE_FLOW = getReceiverId(PipeMessageReceiver.FLOW);

    static {
        for (PipeMessageReceiver rec : PipeMessageReceiver.VALUES) {
            IDS.allocId("UPDATE_" + rec);
        }
    }

    private static int getReceiverId(PipeMessageReceiver type) {
        return NET_UPDATE_MULTI + 1 + type.ordinal();
    }

    /** @return The side of the pluggable that the given message id is for, or null if it isn't for a pluggable. */
    @Nullable
    private static Direction getPlugSide(int id) {
        for (Direction side : Direction.values()) {
            if (id == getReceiverId(PipeMessageReceiver.PLUGGABLES[side.ordinal()])) {
                return side;
            }
        }
        return null;
    }

    public final PipeEventBus eventBus = new PipeEventBus();
    private Pipe pipe;
    private final PipePluggable[] pluggables = new PipePluggable[6];
    private boolean scheduleRenderUpdate = true;
    private final Set<PipeMessageReceiver> networkUpdates = EnumSet.noneOf(PipeMessageReceiver.class);
    private final Set<PipeMessageReceiver> networkGuiUpdates = EnumSet.noneOf(PipeMessageReceiver.class);
    /** The saved pipe, if its definition couldn't be loaded. Kept so that the pipe isn't lost forever. */
    private CompoundTag unknownData;

    public TilePipeHolder(BlockPos pos, BlockState state) {
        super(BCTransportBlocks.PIPE_HOLDER_TILE.get(), pos, state);
        caps.addCapabilityInstance(PipeApi.CAP_PIPE_HOLDER, this, EnumPipePart.VALUES);
        caps.addCapability(PipeApi.CAP_PIPE, () -> pipe, EnumPipePart.VALUES);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    private void setPipe(@Nullable Pipe newPipe) {
        if (pipe != null) {
            eventBus.unregisterHandler(pipe.behaviour);
            eventBus.unregisterHandler(pipe.flow);
        }
        pipe = newPipe;
        if (pipe != null) {
            eventBus.registerHandler(pipe.behaviour);
            eventBus.registerHandler(pipe.flow);
        }
        // The pipe capability now points at a different object
        caps.invalidate();
    }

    // Read + write

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (pipe != null) {
            nbt.put("pipe", pipe.writeToNbt());
        } else if (unknownData != null) {
            nbt.put("pipe", unknownData);
        }
        CompoundTag plugs = new CompoundTag();
        for (Direction side : Direction.values()) {
            PipePluggable plug = pluggables[side.ordinal()];
            if (plug != null) {
                CompoundTag plugTag = new CompoundTag();
                plugTag.putString("id", plug.definition.identifier.toString());
                plugTag.put("data", plug.writeToNbt());
                plugs.put(side.getSerializedName(), plugTag);
            }
        }
        nbt.put("plugs", plugs);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("pipe")) {
            CompoundTag pipeTag = nbt.getCompound("pipe");
            try {
                setPipe(new Pipe(this, pipeTag));
                unknownData = null;
            } catch (IllegalArgumentException e) {
                // Unfortunately we can't throw an exception because then this tile won't persist :/
                BCLog.logger.warn("[transport.pipe] Couldn't load the pipe at {}: {}", worldPosition, e.getMessage());
                unknownData = pipeTag.copy();
            }
        }
        CompoundTag plugs = nbt.getCompound("plugs");
        for (Direction side : Direction.values()) {
            PipePluggable plug = null;
            if (plugs.contains(side.getSerializedName())) {
                CompoundTag plugTag = plugs.getCompound(side.getSerializedName());
                PluggableDefinition definition = PipeApi.pluggableRegistry.getDefinition(
                    ResourceLocation.parse(plugTag.getString("id"))
                );
                if (definition == null) {
                    BCLog.logger.warn("[transport.pipe] Unknown pluggable '{}' at {}", plugTag.getString("id"),
                        worldPosition);
                } else {
                    plug = definition.readFromNbt(this, side, plugTag.getCompound("data"));
                }
            }
            setPluggable(side, plug);
        }
    }

    // Pluggables

    @Nullable
    @Override
    public PipePluggable getPluggable(@Nullable Direction side) {
        return side == null ? null : pluggables[side.ordinal()];
    }

    @Nullable
    @Override
    public PipePluggable replacePluggable(Direction side, @Nullable PipePluggable with) {
        PipePluggable old = setPluggable(side, with);
        if (level != null && !level.isClientSide) {
            sendMessage(PipeMessageReceiver.PLUGGABLES[side.ordinal()], buffer -> writePlugCreation(buffer, with));
            if (pipe != null) {
                pipe.markForUpdate();
            }
            // The pipe next to this one may connect differently now
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            setChanged();
        }
        return old;
    }

    /** Swaps the pluggable on the given side, without telling anyone. */
    private PipePluggable setPluggable(Direction side, @Nullable PipePluggable with) {
        PipePluggable old = pluggables[side.ordinal()];
        if (old != null) {
            eventBus.unregisterHandler(old);
        }
        pluggables[side.ordinal()] = with;
        if (with != null) {
            eventBus.registerHandler(with);
        }
        caps.invalidate();
        scheduleRenderUpdate();
        return old;
    }

    private static void writePlugCreation(FriendlyByteBuf buffer, @Nullable PipePluggable plug) {
        if (plug == null) {
            buffer.writeByte(PLUG_REMOVE);
        } else {
            buffer.writeByte(PLUG_CREATE);
            buffer.writeResourceLocation(plug.definition.identifier);
            plug.writeCreationPayload(buffer);
        }
    }

    private void readPlugMessage(Direction side, FriendlyByteBuf buffer, LogicalSide netSide) {
        int type = buffer.readUnsignedByte();
        if (type == PLUG_REMOVE) {
            setPluggable(side, null);
        } else if (type == PLUG_CREATE) {
            ResourceLocation id = buffer.readResourceLocation();
            PluggableDefinition definition = PipeApi.pluggableRegistry.getDefinition(id);
            if (definition == null) {
                throw new IllegalStateException("Unknown pluggable " + id);
            }
            setPluggable(side, definition.loadFromBuffer(this, side, buffer));
        } else if (type == PLUG_UPDATE) {
            PipePluggable plug = pluggables[side.ordinal()];
            if (plug != null) {
                plug.readPayload(buffer, netSide);
            }
        }
    }

    // Misc

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        Item item = stack.getItem();
        if (item instanceof IItemPipe) {
            PipeDefinition definition = ((IItemPipe) item).getDefinition();
            setPipe(new Pipe(this, definition));
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("colour")) {
                pipe.setColour(DyeColor.byId(tag.getByte("colour")));
            }
        }
        scheduleRenderUpdate();
        if (level != null && !level.isClientSide) {
            sendNetworkUpdate(NET_RENDER_DATA);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (pipe != null) {
            pipe.onLoad();
        }
    }

    @Override
    public void onNeighbourBlockChanged(Block block, BlockPos neighbour) {
        super.onNeighbourBlockChanged(block, neighbour);
        if (level.isClientSide) {
            return;
        }
        if (pipe != null) {
            pipe.markForUpdate();
        }
    }

    /** Adds the pipe item, and everything inside the pipe. */
    public void addDrops(NonNullList<ItemStack> toDrop) {
        for (PipePluggable plug : pluggables) {
            if (plug != null) {
                plug.addDrops(toDrop);
            }
        }
        if (pipe != null) {
            pipe.addDrops(toDrop);
        }
    }

    // ITickable

    @Override
    public void tick() {
        if (pipe != null) {
            pipe.onTick();
        }
        for (PipePluggable plug : pluggables) {
            if (plug != null) {
                plug.onTick();
            }
        }

        // Send network updates
        if (!networkUpdates.isEmpty()) {
            for (PipeMessageReceiver part : EnumSet.copyOf(networkUpdates)) {
                sendNetworkUpdate(getReceiverId(part));
            }
        }
        // No need to send gui updates to specific players if we just sent off messages to all players.
        networkGuiUpdates.removeAll(networkUpdates);
        networkUpdates.clear();

        if (!networkGuiUpdates.isEmpty()) {
            for (PipeMessageReceiver part : EnumSet.copyOf(networkGuiUpdates)) {
                sendNetworkGuiUpdate(getReceiverId(part));
            }
        }
        networkGuiUpdates.clear();

        if (scheduleRenderUpdate) {
            scheduleRenderUpdate = false;
            redrawBlock();
        }

        if (!level.isClientSide) {
            /* It's difficult to check to see if we actually have changed at all. So let's just always mark the chunk
             * as dirty instead of making every component do it individually. */
            markChunkDirty();
        }
    }

    // Network

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                if (pipe == null) {
                    buffer.writeBoolean(false);
                } else {
                    buffer.writeBoolean(true);
                    pipe.writeCreationPayload(buffer);
                }
                for (PipePluggable plug : pluggables) {
                    writePlugCreation(buffer, plug);
                }
            } else if (getPlugSide(id) != null) {
                PipePluggable plug = pluggables[getPlugSide(id).ordinal()];
                if (plug == null) {
                    buffer.writeByte(PLUG_REMOVE);
                } else {
                    buffer.writeByte(PLUG_UPDATE);
                    plug.writePayload(buffer, side);
                }
            } else if (id == NET_UPDATE_PIPE_BEHAVIOUR) {
                if (pipe == null) {
                    buffer.writeBoolean(false);
                } else {
                    buffer.writeBoolean(true);
                    pipe.writePayload(buffer, side);
                }
            }
        }
        if (id == NET_UPDATE_PIPE_FLOW) {
            if (pipe == null || pipe.flow == null) {
                buffer.writeBoolean(false);
            } else {
                buffer.writeBoolean(true);
                buffer.writeShort(PipeFlow.NET_ID_UPDATE);
                pipe.flow.writePayload(PipeFlow.NET_ID_UPDATE, buffer, side);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                if (buffer.readBoolean()) {
                    setPipe(new Pipe(this, buffer));
                } else if (pipe != null) {
                    setPipe(null);
                }
                for (Direction face : Direction.values()) {
                    readPlugMessage(face, buffer, side);
                }
                scheduleRenderUpdate();
            } else if (getPlugSide(id) != null) {
                readPlugMessage(getPlugSide(id), buffer, side);
            } else if (id == NET_UPDATE_PIPE_BEHAVIOUR) {
                if (buffer.readBoolean()) {
                    if (pipe == null) {
                        throw new IllegalStateException("Pipe was null when it shouldn't have been!");
                    } else {
                        pipe.readPayload(buffer, side);
                    }
                }
            }
        }
        if (id == NET_UPDATE_PIPE_FLOW) {
            if (buffer.readBoolean()) {
                if (pipe == null) {
                    throw new IllegalStateException("Pipe was null when it shouldn't have been!");
                } else {
                    int fId = buffer.readShort();
                    pipe.flow.readPayload(fId, buffer, side);
                }
            }
        }
    }

    // IPipeHolder

    @Override
    public Level getPipeWorld() {
        return level;
    }

    @Override
    public BlockPos getPipePos() {
        return worldPosition;
    }

    @Override
    public BlockEntity getPipeTile() {
        return this;
    }

    @Override
    public Pipe getPipe() {
        return pipe;
    }

    @Override
    public boolean canPlayerInteract(Player player) {
        return canInteractWith(player);
    }

    @Override
    public IPipe getNeighbourPipe(Direction side) {
        BlockEntity neighbour = getNeighbourTile(side);
        if (neighbour == null) {
            return null;
        }
        return neighbour.getCapability(PipeApi.CAP_PIPE, side.getOpposite()).orElse(null);
    }

    @Override
    public <T> T getCapabilityFromPipe(Direction side, @Nonnull Capability<T> capability) {
        if (pipe != null && pipe.isConnected(side)) {
            BlockEntity neighbour = getNeighbourTile(side);
            if (neighbour != null) {
                return neighbour.getCapability(capability, side.getOpposite()).orElse(null);
            }
        }
        return null;
    }

    @Override
    public void scheduleRenderUpdate() {
        scheduleRenderUpdate = true;
    }

    @Override
    public void scheduleNetworkUpdate(PipeMessageReceiver... parts) {
        Collections.addAll(networkUpdates, parts);
    }

    @Override
    public void scheduleNetworkGuiUpdate(PipeMessageReceiver... parts) {
        Collections.addAll(networkGuiUpdates, parts);
    }

    @Override
    public void sendMessage(PipeMessageReceiver to, IWriter writer) {
        createAndSendMessage(getReceiverId(to), writer::write);
    }

    @Override
    public void sendGuiMessage(PipeMessageReceiver to, IWriter writer) {
        createAndSendGuiMessage(getReceiverId(to), writer::write);
    }

    @Override
    public boolean fireEvent(PipeEvent event) {
        return eventBus.fireEvent(event);
    }

    // Caps

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        PipePluggable plug = getPluggable(facing);
        if (plug != null) {
            LazyOptional<T> val = plug.getCapability(capability);
            if (val.isPresent()) {
                return val;
            }
            if (plug.isBlocking()) {
                return LazyOptional.empty();
            }
        }
        if (pipe != null && capability != PipeApi.CAP_PIPE && capability != PipeApi.CAP_PIPE_HOLDER) {
            LazyOptional<T> val = pipe.getCapability(capability, facing);
            if (val.isPresent()) {
                return val;
            }
        }
        return super.getCapability(capability, facing);
    }

    // Client side stuffs

    @Nonnull
    @Override
    public ModelData getModelData() {
        PluggableModelKey[] plugKeys = new PluggableModelKey[6];
        for (int i = 0; i < 6; i++) {
            plugKeys[i] = pluggables[i] == null ? null : pluggables[i].getModelRenderKey();
        }
        return ModelData.builder().with(MODEL_KEY, pipe == null ? PipeModelKey.DEFAULT_KEY : pipe.getModel())
            .with(PLUG_KEYS, plugKeys).build();
    }
}
