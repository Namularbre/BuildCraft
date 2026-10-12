/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import java.util.EnumMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.transport.pipe.ICustomPipeConnection;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeConnectionAPI;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeEventConnectionChange;
import buildcraft.api.transport.pipe.PipeFaceTex;
import buildcraft.api.transport.pipe.PipeFlow;

/** A pipe inside a {@link IPipeHolder}: its definition, plus the behaviour and flow that the definition created. */
public final class Pipe implements IPipe {
    private static final float DEFAULT_CONNECTION_DISTANCE = 0.25f;

    public final IPipeHolder holder;
    public final PipeDefinition definition;
    public final PipeBehaviour behaviour;
    public final PipeFlow flow;
    private DyeColor colour = null;
    private boolean updateMarked = true;
    private final EnumMap<Direction, Float> connected = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, ConnectedType> types = new EnumMap<>(Direction.class);

    public Pipe(IPipeHolder holder, PipeDefinition definition) {
        this.holder = holder;
        this.definition = definition;
        this.behaviour = definition.logicConstructor.createBehaviour(this);
        this.flow = definition.flowType.creator.createFlow(this);
    }

    // read + write

    /** @throws IllegalArgumentException if the pipe definition isn't known (for example if it was removed). */
    public Pipe(IPipeHolder holder, CompoundTag nbt) {
        this.holder = holder;
        int col = nbt.getByte("col");
        this.colour = col == 0 ? null : DyeColor.byId(col - 1);
        this.definition = PipeRegistry.INSTANCE.loadDefinition(nbt.getString("def"));
        if (!definition.canBeColoured) {
            colour = null;
        }
        this.behaviour = definition.logicLoader.loadBehaviour(this, nbt.getCompound("beh"));
        this.flow = definition.flowType.loader.loadFlow(this, nbt.getCompound("flow"));

        int connectionData = nbt.getInt("con");
        for (Direction face : Direction.values()) {
            int data = (connectionData >>> (face.ordinal() * 2)) & 0b11;
            // The only important aspect of this is the pipe type
            // as the texture index is just used at the client (which is updated in the first tick)
            // and the distance is only used on the server for item pipe travel times.
            // (which is minor enough that it doesn't really matter)
            if (data == 0b01) {
                connected.put(face, DEFAULT_CONNECTION_DISTANCE);
                types.put(face, ConnectedType.PIPE);
            } else if (data == 0b10) {
                connected.put(face, DEFAULT_CONNECTION_DISTANCE);
                types.put(face, ConnectedType.TILE);
            }
        }
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        if (colour != null) {
            nbt.putByte("col", (byte) (colour.getId() + 1));
        }
        nbt.putString("def", definition.identifier.toString());
        nbt.put("beh", behaviour.writeToNbt());
        nbt.put("flow", flow.writeToNbt());

        int connectionData = 0;
        for (Direction face : Direction.values()) {
            ConnectedType type = types.get(face);
            if (type != null) {
                int data = type == ConnectedType.PIPE ? 0b01 : 0b10;
                connectionData |= data << (face.ordinal() * 2);
            }
        }
        nbt.putInt("con", connectionData);
        return nbt;
    }

    // network

    /** Reads a pipe created by {@link #writeCreationPayload(FriendlyByteBuf)}, on the client. */
    public Pipe(IPipeHolder holder, FriendlyByteBuf buffer) {
        this.holder = holder;
        this.definition = PipeRegistry.INSTANCE.loadDefinition(buffer.readUtf(256));
        this.behaviour = definition.logicConstructor.createBehaviour(this);
        readPayload(buffer, LogicalSide.CLIENT);
        this.flow = definition.flowType.creator.createFlow(this);
        this.flow.readPayload(PipeFlow.NET_ID_FULL_STATE, buffer, LogicalSide.CLIENT);
    }

    public void writeCreationPayload(FriendlyByteBuf buffer) {
        buffer.writeUtf(definition.identifier.toString(), 256);
        writePayload(buffer, LogicalSide.SERVER);
        flow.writePayload(PipeFlow.NET_ID_FULL_STATE, buffer, LogicalSide.SERVER);
    }

    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {
        if (side == LogicalSide.SERVER) {
            buffer.writeByte(colour == null ? 0 : colour.getId() + 1);
            for (Direction face : Direction.values()) {
                Float con = connected.get(face);
                if (con != null) {
                    buffer.writeBoolean(true);
                    buffer.writeFloat(con);
                    buffer.writeEnum(types.get(face));
                } else {
                    buffer.writeBoolean(false);
                }
            }
            behaviour.writePayload(buffer, side);
        }
    }

    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        if (side == LogicalSide.CLIENT) {
            connected.clear();
            types.clear();

            int nColour = buffer.readUnsignedByte();
            colour = nColour == 0 ? null : DyeColor.byId(nColour - 1);

            for (Direction face : Direction.values()) {
                if (buffer.readBoolean()) {
                    connected.put(face, buffer.readFloat());
                    types.put(face, buffer.readEnum(ConnectedType.class));
                }
            }

            behaviour.readPayload(buffer, side);
            getHolder().scheduleRenderUpdate();
        }
    }

    // IPipe

    @Override
    public IPipeHolder getHolder() {
        return holder;
    }

    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }

    @Override
    public PipeBehaviour getBehaviour() {
        return behaviour;
    }

    @Override
    public PipeFlow getFlow() {
        return flow;
    }

    @Override
    public DyeColor getColour() {
        return this.colour;
    }

    @Override
    public void setColour(DyeColor colour) {
        if (definition.canBeColoured) {
            this.colour = colour;
            markForUpdate();
        }
    }

    // Caps

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        LazyOptional<T> val = behaviour.getCapability(capability, facing);
        if (val.isPresent()) return val;
        return flow.getCapability(capability, facing);
    }

    // misc

    public void onLoad() {
        markForUpdate();
    }

    public void onTick() {
        if (updateMarked) {
            // Ensure that the behaviour and flow *always* get valid connection data
            // (for example if we just read from disk)
            updateConnections();
        }
        behaviour.onTick();
        flow.onTick();
        if (updateMarked) {
            updateConnections();
        }
    }

    private void updateConnections() {
        if (holder.getPipeWorld().isClientSide) {
            return;
        }
        updateMarked = false;

        EnumMap<Direction, Float> old = connected.clone();

        connected.clear();
        types.clear();

        for (Direction facing : Direction.values()) {
            BlockEntity oTile = getHolder().getNeighbourTile(facing);
            if (oTile == null) {
                continue;
            }
            IPipe oPipe = getHolder().getNeighbourPipe(facing);
            if (oPipe != null) {
                if (oPipe.getBehaviour() != null && canPipesConnect(facing, this, oPipe)) {
                    connected.put(facing, DEFAULT_CONNECTION_DISTANCE);
                    types.put(facing, ConnectedType.PIPE);
                }
                continue;
            }

            BlockPos nPos = holder.getPipePos().relative(facing);
            BlockState neighbour = holder.getPipeWorld().getBlockState(nPos);

            ICustomPipeConnection cust = PipeConnectionAPI.getCustomConnection(neighbour.getBlock());
            if (cust == null) {
                cust = DefaultPipeConnection.INSTANCE;
            }
            float ext = DEFAULT_CONNECTION_DISTANCE
                + cust.getExtension(holder.getPipeWorld(), nPos, facing.getOpposite(), neighbour);

            if (behaviour.shouldForceConnection(facing, oTile) || flow.shouldForceConnection(facing, oTile)
                || (behaviour.canConnect(facing, oTile) && flow.canConnect(facing, oTile))) {
                connected.put(facing, ext);
                types.put(facing, ConnectedType.TILE);
            }
        }
        if (!old.equals(connected)) {
            for (Direction face : Direction.values()) {
                boolean o = old.containsKey(face);
                boolean n = connected.containsKey(face);
                if (o != n) {
                    IPipe oPipe = getHolder().getNeighbourPipe(face);
                    if (oPipe != null) {
                        oPipe.markForUpdate();
                    }
                    holder.fireEvent(new PipeEventConnectionChange(holder, face));
                }
            }
        }
        getHolder().scheduleNetworkUpdate(PipeMessageReceiver.BEHAVIOUR);
    }

    public void addDrops(NonNullList<ItemStack> toDrop) {
        Item item = (Item) PipeApi.pipeRegistry.getItemForPipe(definition);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            if (colour != null) {
                stack.getOrCreateTag().putByte("colour", (byte) colour.getId());
            }
            toDrop.add(stack);
        }
        flow.addDrops(toDrop, 0);
        behaviour.addDrops(toDrop, 0);
    }

    public static boolean canPipesConnect(Direction to, IPipe one, IPipe two) {
        return canColoursConnect(one.getColour(), two.getColour())//
            && canBehavioursConnect(to, one.getBehaviour(), two.getBehaviour())//
            && canFlowsConnect(to, one.getFlow(), two.getFlow());
    }

    public static boolean canColoursConnect(DyeColor one, DyeColor two) {
        return one == null || two == null || one == two;
    }

    public static boolean canBehavioursConnect(Direction to, PipeBehaviour one, PipeBehaviour two) {
        return one.canConnect(to, two) && two.canConnect(to.getOpposite(), one);
    }

    public static boolean canFlowsConnect(Direction to, PipeFlow one, PipeFlow two) {
        return one.canConnect(to, two) && two.canConnect(to.getOpposite(), one);
    }

    @Override
    public void markForUpdate() {
        updateMarked = true;
    }

    /** @return Everything that the pipe model depends on, for the client. */
    public PipeModelKey getModel() {
        PipeFaceTex[] sides = new PipeFaceTex[6];
        float[] mc = new float[6];
        for (Direction face : Direction.values()) {
            int i = face.ordinal();
            sides[i] = behaviour.getTextureData(face);
            mc[i] = getConnectedDist(face);
        }
        return new PipeModelKey(definition, behaviour.getTextureData(null), sides, mc, colour);
    }

    @Override
    public BlockEntity getConnectedTile(Direction side) {
        if (connected.containsKey(side)) {
            BlockEntity offset = getHolder().getNeighbourTile(side);
            if (offset == null && !getHolder().getPipeWorld().isClientSide) {
                markForUpdate();
            } else {
                return offset;
            }
        }
        return null;
    }

    @Override
    public IPipe getConnectedPipe(Direction side) {
        if (connected.containsKey(side) && getConnectedType(side) == ConnectedType.PIPE) {
            IPipe offset = getHolder().getNeighbourPipe(side);
            if (offset == null && !getHolder().getPipeWorld().isClientSide) {
                markForUpdate();
            } else {
                return offset;
            }
        }
        return null;
    }

    @Override
    public ConnectedType getConnectedType(Direction side) {
        return types.get(side);
    }

    @Override
    public boolean isConnected(Direction side) {
        return connected.containsKey(side);
    }

    public float getConnectedDist(Direction face) {
        Float custom = connected.get(face);
        return custom == null ? 0 : custom;
    }
}
