/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.transport.pluggable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.IPipeHolder.IWriter;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;

/** Something attached to one side of a pipe (like a blocker, a gate or a facade). Pluggables also receive the pipe's
 * events, like behaviours and flows. */
public abstract class PipePluggable {
    /** The type of the messages of the pipe holder that are passed on to {@link #readPayload}. */
    public static final int NET_UPDATE = 2;

    public final PluggableDefinition definition;
    public final IPipeHolder holder;
    public final Direction side;

    public PipePluggable(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        this.definition = definition;
        this.holder = holder;
        this.side = side;
    }

    public CompoundTag writeToNbt() {
        return new CompoundTag();
    }

    /** Writes the payload that will be passed into
     * {@link PluggableDefinition#loadFromBuffer(IPipeHolder, Direction, FriendlyByteBuf)} on the client. */
    public void writeCreationPayload(FriendlyByteBuf buffer) {}

    public void writePayload(FriendlyByteBuf buffer, LogicalSide side) {}

    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {}

    public final void scheduleNetworkUpdate() {
        holder.scheduleNetworkUpdate(PipeMessageReceiver.PLUGGABLES[side.ordinal()]);
    }

    /** Sends a message to this pluggable on every client watching the pipe, which reads it in {@link #readPayload}. */
    public final void sendMessage(IWriter writer) {
        holder.sendMessage(PipeMessageReceiver.PLUGGABLES[side.ordinal()], buffer -> {
            buffer.writeByte(NET_UPDATE);
            writer.write(buffer);
        });
    }

    /** Like {@link #sendMessage(IWriter)}, but only to the players that have a GUI of the pipe open. */
    public final void sendGuiMessage(IWriter writer) {
        holder.sendGuiMessage(PipeMessageReceiver.PLUGGABLES[side.ordinal()], buffer -> {
            buffer.writeByte(NET_UPDATE);
            writer.write(buffer);
        });
    }

    public void onTick() {}

    /** @return The box of this pluggable, in block coordinates (from 0 to 1). */
    public abstract AABB getBoundingBox();

    /** @return True if this stops the pipe from connecting on its side. */
    public boolean isBlocking() {
        return false;
    }

    /** @return The capability that other blocks get on this side of the pipe, or an empty one to fall back to the
     *         pipe's own (unless this is {@link #isBlocking() blocking}). */
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        return LazyOptional.empty();
    }

    /** Called when this pluggable is removed from the pipe (but not when the pipe is removed). */
    public void onRemove() {}

    public void addDrops(NonNullList<ItemStack> toDrop) {
        ItemStack stack = getPickStack();
        if (!stack.isEmpty()) {
            toDrop.add(stack);
        }
    }

    /** @return The item that this pluggable comes from (what pick block gives, and what it drops). */
    public ItemStack getPickStack() {
        return ItemStack.EMPTY;
    }

    public boolean onPluggableActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ) {
        return false;
    }

    /** @return The key of the model of this pluggable (drawn as part of the pipe's model), or null if it isn't drawn
     *         that way. */
    @Nullable
    public PluggableModelKey getModelRenderKey() {
        return null;
    }

    /** @return True if redstone dust next to this side of the pipe should connect to it. */
    public boolean canConnectToRedstone() {
        return false;
    }

    public void onPlacedBy(Player player) {}
}
