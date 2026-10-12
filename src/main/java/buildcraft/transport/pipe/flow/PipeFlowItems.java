/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.flow;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.api.core.IStackFilter;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipe.ConnectedType;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.api.transport.pipe.PipeFlow;

import buildcraft.lib.inventory.ItemTransactorHelper;
import buildcraft.lib.misc.data.DelayedList;

import buildcraft.transport.pipe.behaviour.PipeBehaviourStone;

/** Moves items through pipes. Items travel from the side they entered to the center, where events decide where they
 * should go next, and then to the side they leave through. */
public final class PipeFlowItems extends PipeFlow implements IFlowItems {
    private static final double EXTRACT_SPEED = 0.08;
    public static final int NET_CREATE_ITEM = 2;

    private final DelayedList<TravellingItem> items = new DelayedList<>();
    private final LazyOptional<IInjectable> injectable = LazyOptional.of(() -> this);
    /** True if the items were just loaded with timings relative to 0, rather than to the level's game time. */
    private boolean needsTimeRebase;

    public PipeFlowItems(IPipe pipe) {
        super(pipe);
    }

    public PipeFlowItems(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        ListTag list = nbt.getList("items", Tag.TAG_COMPOUND);
        // The pipe's level isn't set yet when loading, so save item timings relative to 0
        for (int i = 0; i < list.size(); i++) {
            TravellingItem item = new TravellingItem(list.getCompound(i), 0);
            if (!item.stack.isEmpty()) {
                items.add(item.getCurrentDelay(0), item);
            }
        }
        // Re-base the timings onto the level's time on the first tick
        needsTimeRebase = list.size() > 0;
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        ListTag list = new ListTag();

        long tickNow = needsTimeRebase ? 0 : pipe.getHolder().getPipeWorld().getGameTime();
        for (List<TravellingItem> l : items.getAllElements()) {
            for (TravellingItem item : l) {
                list.add(item.writeToNbt(tickNow));
            }
        }
        nbt.put("items", list);
        return nbt;
    }

    // Network

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide msgSide) {
        if (msgSide == LogicalSide.CLIENT) {
            if (id == NET_CREATE_ITEM) {
                TravellingItem item = new TravellingItem(buffer.readItem());
                item.toCenter = buffer.readBoolean();
                item.side = buffer.readEnum(Direction.class);
                int colour = buffer.readByte();
                item.colour = colour < 0 ? null : DyeColor.byId(colour);
                item.timeToDest = buffer.readUnsignedShort();
                item.tickStarted = pipe.getHolder().getPipeWorld().getGameTime() + 1;
                item.tickFinished = item.tickStarted + item.timeToDest;
                items.add(item.timeToDest + 1, item);
            }
        }
    }

    void sendItemDataToClient(TravellingItem item) {
        sendCustomPayload(NET_CREATE_ITEM, (buffer) -> {
            buffer.writeItem(item.stack);
            buffer.writeBoolean(item.toCenter);
            buffer.writeEnum(item.side);
            buffer.writeByte(item.colour == null ? -1 : item.colour.getId());
            buffer.writeShort(Math.min(item.timeToDest, 0xFFFF));
        });
    }

    @Override
    public void addDrops(NonNullList<ItemStack> toDrop, int fortune) {
        super.addDrops(toDrop, fortune);
        for (List<TravellingItem> list : items.getAllElements()) {
            for (TravellingItem item : list) {
                if (!item.isPhantom) {
                    toDrop.add(item.stack);
                }
            }
        }
    }

    // IFlowItems

    @Override
    public int tryExtractItems(int count, Direction from, DyeColor colour, IStackFilter filter, boolean simulate) {
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            throw new IllegalStateException("Cannot extract items on the client side!");
        }
        if (from == null) {
            return 0;
        }

        BlockEntity tile = pipe.getConnectedTile(from);
        IItemHandler handler = ItemTransactorHelper.getItemHandler(tile, from.getOpposite());

        ItemStack possible = ItemTransactorHelper.extract(handler, filter, 1, count, true);

        if (possible.isEmpty()) {
            return 0;
        }
        if (possible.getCount() > possible.getMaxStackSize()) {
            possible.setCount(possible.getMaxStackSize());
            count = possible.getMaxStackSize();
        }

        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.TryInsert tryInsert = new PipeEventItem.TryInsert(holder, this, colour, from, possible);
        holder.fireEvent(tryInsert);
        if (tryInsert.isCanceled() || tryInsert.accepted <= 0) {
            return 0;
        }

        count = Math.min(count, tryInsert.accepted);

        ItemStack stack = ItemTransactorHelper.extract(handler, filter, count, count, simulate);

        if (stack.isEmpty()) {
            throw new IllegalStateException(
                "The item handler " + handler + " returned an empty itemstack from a known good request!"
            );
        }

        if (!simulate) {
            insertItemEvents(stack, colour, EXTRACT_SPEED, from);
        }

        return count;
    }

    @Override
    public void sendPhantomItem(ItemStack stack, Direction from, Direction to, DyeColor colour) {
        if (from == null && to == null) {
            return;
        }
        Direction face0, face1, face2;
        boolean twoItems = from != null && to != null;
        face0 = from;
        face1 = from == null ? to : null;
        face2 = to;

        long now = pipe.getHolder().getPipeWorld().getGameTime();

        TravellingItem firstItem = new TravellingItem(stack);
        firstItem.isPhantom = true;
        firstItem.toCenter = face1 == null;
        firstItem.colour = colour;
        firstItem.side = face0 == null ? face1 : face0;
        firstItem.speed = EXTRACT_SPEED;
        firstItem.genTimings(now, getPipeLength(firstItem.side));
        items.add(firstItem.timeToDest, firstItem);
        sendItemDataToClient(firstItem);

        if (twoItems) {
            TravellingItem secondItem = new TravellingItem(stack);
            secondItem.isPhantom = true;
            secondItem.toCenter = false;
            secondItem.colour = colour;
            secondItem.side = face2;
            secondItem.speed = EXTRACT_SPEED;
            secondItem.genTimings(firstItem.tickFinished, getPipeLength(secondItem.side));
            items.add(secondItem.timeToDest, secondItem);
            sendItemDataToClient(secondItem);
        }
    }

    // PipeFlow

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == PipeApi.CAP_INJECTABLE) {
            return injectable.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean canConnect(Direction face, PipeFlow other) {
        return other instanceof IFlowItems;
    }

    @Override
    public boolean canConnect(Direction face, BlockEntity oTile) {
        return ItemTransactorHelper.getItemHandler(oTile, face.getOpposite()) != null
            || ItemTransactorHelper.getInjectable(oTile, face.getOpposite()) != null;
    }

    @Override
    public void onTick() {
        Level world = pipe.getHolder().getPipeWorld();
        long currentTime = world.getGameTime();

        if (needsTimeRebase) {
            needsTimeRebase = false;
            for (List<TravellingItem> list : items.getAllElements()) {
                for (TravellingItem item : list) {
                    item.tickStarted += currentTime;
                    item.tickFinished += currentTime;
                }
            }
        }

        List<TravellingItem> toTick = items.advance();

        for (TravellingItem item : toTick) {
            if (item.tickFinished > currentTime) {
                // Can happen if something ticks this tile multiple times in a single real tick
                items.add((int) (item.tickFinished - currentTime), item);
                continue;
            }
            if (item.isPhantom) {
                continue;
            }
            if (world.isClientSide) {
                continue;
            }
            if (item.toCenter) {
                onItemReachCenter(item);
            } else {
                onItemReachEnd(item);
            }
        }
    }

    private void onItemReachCenter(TravellingItem item) {
        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.ReachCenter reachCenter = new PipeEventItem.ReachCenter(
            holder, this, item.colour, item.stack, item.side
        );
        holder.fireEvent(reachCenter);
        if (reachCenter.getStack().isEmpty()) {
            return;
        }

        PipeEventItem.SideCheck sideCheck = new PipeEventItem.SideCheck(
            holder, this, reachCenter.colour, reachCenter.from, reachCenter.getStack()
        );
        sideCheck.disallow(reachCenter.from);
        for (Direction face : Direction.values()) {
            if (item.tried.contains(face) || !pipe.isConnected(face)) {
                sideCheck.disallow(face);
            }
        }
        holder.fireEvent(sideCheck);

        List<EnumSet<Direction>> order = sideCheck.getOrder();
        if (order.isEmpty()) {
            PipeEventItem.TryBounce tryBounce = new PipeEventItem.TryBounce(
                holder, this, reachCenter.colour, reachCenter.from, reachCenter.getStack()
            );
            holder.fireEvent(tryBounce);
            if (tryBounce.canBounce) {
                order = ImmutableList.of(EnumSet.of(reachCenter.from));
            } else {
                dropItem(item.stack, item.side.getOpposite(), item.speed);
                return;
            }
        }

        PipeEventItem.ItemEntry entry = new PipeEventItem.ItemEntry(
            reachCenter.colour, reachCenter.getStack(), reachCenter.from
        );
        PipeEventItem.Split split = new PipeEventItem.Split(holder, this, order, entry);
        holder.fireEvent(split);
        ImmutableList<PipeEventItem.ItemEntry> entries = ImmutableList.copyOf(split.items);

        PipeEventItem.FindDest findDest = new PipeEventItem.FindDest(holder, this, order, entries);
        holder.fireEvent(findDest);

        long now = holder.getPipeWorld().getGameTime();
        for (PipeEventItem.ItemEntry itemEntry : findDest.items) {
            if (itemEntry.stack.isEmpty()) {
                continue;
            }
            PipeEventItem.ModifySpeed modifySpeed = new PipeEventItem.ModifySpeed(holder, this, itemEntry, item.speed);

            final double newSpeed;

            if (holder.fireEvent(modifySpeed)) {
                double target = modifySpeed.targetSpeed;
                double maxDelta = modifySpeed.maxSpeedChange;
                if (item.speed < target) {
                    newSpeed = Math.min(target, item.speed + maxDelta);
                } else if (item.speed > target) {
                    newSpeed = Math.max(target, item.speed - maxDelta);
                } else {
                    newSpeed = item.speed;
                }
            } else {
                // Nothing affected the speed
                // so just fallback to a sensible default
                if (item.speed > 0.03) {
                    newSpeed = Math.max(0.03, item.speed - PipeBehaviourStone.SPEED_DELTA);
                } else {
                    newSpeed = item.speed;
                }
            }

            List<Direction> destinations = itemEntry.to;
            if (destinations == null || destinations.size() == 0) {
                destinations = findDest.generateRandomOrder();
            }
            if (destinations.size() == 0) {
                dropItem(itemEntry.stack, item.side.getOpposite(), newSpeed);
            } else {
                TravellingItem newItem = new TravellingItem(itemEntry.stack);
                newItem.tried.addAll(item.tried);
                newItem.toCenter = false;
                newItem.colour = itemEntry.colour;
                newItem.side = destinations.get(0);
                newItem.speed = newSpeed;
                newItem.genTimings(now, getPipeLength(newItem.side));
                items.add(newItem.timeToDest, newItem);
                sendItemDataToClient(newItem);
            }
        }
    }

    private void onItemReachEnd(TravellingItem item) {
        IPipeHolder holder = pipe.getHolder();
        PipeEventItem.ReachEnd reachEnd = new PipeEventItem.ReachEnd(holder, this, item.colour, item.stack, item.side);
        holder.fireEvent(reachEnd);
        item.colour = reachEnd.colour;
        item.stack = reachEnd.getStack();
        ItemStack excess = item.stack;
        if (excess.isEmpty()) {
            return;
        }
        if (pipe.isConnected(item.side)) {
            ConnectedType type = pipe.getConnectedType(item.side);
            Direction oppositeSide = item.side.getOpposite();
            switch (type) {
                case PIPE: {
                    IPipe oPipe = pipe.getConnectedPipe(item.side);
                    if (oPipe == null) {
                        break;
                    }
                    PipeFlow flow = oPipe.getFlow();
                    if (flow instanceof IFlowItems oFlow) {
                        ItemStack before = excess;
                        excess = oFlow.injectItem(excess.copy(), true, oppositeSide, item.colour, item.speed);

                        if (!excess.isEmpty()) {
                            before.shrink(excess.getCount());
                        }

                        excess = fireEventEjectIntoPipe(oFlow, item.side, before, excess);
                    }
                    break;
                }
                case TILE: {
                    BlockEntity tile = pipe.getConnectedTile(item.side);
                    ItemStack before = excess;
                    IInjectable injectable = ItemTransactorHelper.getInjectable(tile, oppositeSide);
                    if (injectable != null) {
                        excess = injectable.injectItem(excess.copy(), true, oppositeSide, item.colour, item.speed);
                    }
                    if (!excess.isEmpty()) {
                        IItemHandler handler = ItemTransactorHelper.getItemHandler(tile, oppositeSide);
                        if (handler != null) {
                            excess = ItemHandlerHelper.insertItemStacked(handler, excess.copy(), false);
                        }
                    }
                    excess = fireEventEjectIntoTile(tile, item.side, before, excess);
                    break;
                }
            }
        }
        if (excess.isEmpty()) {
            return;
        }
        item.tried.add(item.side);
        item.toCenter = true;
        item.stack = excess;
        item.genTimings(holder.getPipeWorld().getGameTime(), getPipeLength(item.side));
        items.add(item.timeToDest, item);
        sendItemDataToClient(item);
    }

    private ItemStack fireEventEjectIntoPipe(IFlowItems oFlow, Direction to, ItemStack before, ItemStack excess) {
        IPipeHolder holder = this.pipe.getHolder();
        return fireEventEjected(holder, new PipeEventItem.Ejected.IntoPipe(holder, this, before, excess, to, oFlow));
    }

    private ItemStack fireEventEjectIntoTile(BlockEntity tile, Direction to, ItemStack before, ItemStack excess) {
        IPipeHolder holder = this.pipe.getHolder();
        return fireEventEjected(holder, new PipeEventItem.Ejected.IntoTile(holder, this, before, excess, to, tile));
    }

    private static ItemStack fireEventEjected(IPipeHolder holder, PipeEventItem.Ejected event) {
        holder.fireEvent(event);
        return event.getExcess();
    }

    private void dropItem(ItemStack stack, Direction motion, double speed) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        IPipeHolder holder = pipe.getHolder();
        Level world = holder.getPipeWorld();
        BlockPos pos = holder.getPipePos();

        double x = pos.getX() + 0.5 + motion.getStepX() * 0.5;
        double y = pos.getY() + 0.5 + motion.getStepY() * 0.5;
        double z = pos.getZ() + 0.5 + motion.getStepZ() * 0.5;
        speed += 0.01;
        speed *= 2;
        ItemEntity ent = new ItemEntity(world, x, y, z, stack);
        ent.setDeltaMovement(motion.getStepX() * speed, motion.getStepY() * speed, motion.getStepZ() * speed);

        PipeEventItem.Drop drop = new PipeEventItem.Drop(holder, this, ent);
        holder.fireEvent(drop);
        if (ent.getItem().isEmpty() || ent.isRemoved()) {
            return;
        }

        world.addFreshEntity(ent);
    }

    @Override
    public boolean canInjectItems(Direction from) {
        return pipe.isConnected(from);
    }

    @Nonnull
    @Override
    public ItemStack injectItem(@Nonnull ItemStack stack, boolean doAdd, Direction from, DyeColor colour,
        double speed) {
        if (pipe.getHolder().getPipeWorld().isClientSide) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (!canInjectItems(from)) {
            return stack;
        }

        if (speed < 0.01) {
            speed = 0.01;
        }

        // Try insert

        PipeEventItem.TryInsert tryInsert = new PipeEventItem.TryInsert(pipe.getHolder(), this, colour, from, stack);
        pipe.getHolder().fireEvent(tryInsert);
        if (tryInsert.isCanceled() || tryInsert.accepted <= 0) {
            return stack;
        }
        ItemStack toSplit = stack.copy();
        ItemStack toInsert = toSplit.split(tryInsert.accepted);

        if (doAdd) {
            insertItemEvents(toInsert, colour, speed, from);
        }

        if (toSplit.isEmpty()) {
            toSplit = ItemStack.EMPTY;
        }

        return toSplit;
    }

    @Override
    public void insertItemsForce(@Nonnull ItemStack stack, Direction from, DyeColor colour, double speed) {
        Level world = pipe.getHolder().getPipeWorld();
        if (world.isClientSide) {
            throw new IllegalStateException("Cannot inject items on the client side!");
        }
        if (stack.isEmpty()) {
            return;
        }
        if (speed < 0.01) {
            speed = 0.01;
        }
        long now = world.getGameTime();
        TravellingItem item = new TravellingItem(stack);
        if (from == null) {
            // Find a reasonable alternative (as it's not allowed to be null)
            for (Direction f : Direction.values()) {
                if (!pipe.isConnected(f)) {
                    item.side = f;
                    break;
                }
            }
            if (item.side == null) {
                item.side = Direction.UP;
            }
        } else {
            item.side = from;
        }
        item.toCenter = true;
        item.speed = speed;
        item.colour = colour;
        item.genTimings(now, 0);
        if (from != null) {
            item.tried.add(from);
        }
        // Explicitly don't send this item to the client:
        // There's little point in trying to render it
        // seeing as it needs to travel 0 distance.
        items.add(item.timeToDest, item);
    }

    /** Used internally to split up manual insertions from controlled extractions. */
    private void insertItemEvents(@Nonnull ItemStack toInsert, DyeColor colour, double speed, Direction from) {
        IPipeHolder holder = pipe.getHolder();

        PipeEventItem.OnInsert onInsert = new PipeEventItem.OnInsert(holder, this, colour, toInsert, from);
        holder.fireEvent(onInsert);

        if (onInsert.getStack().isEmpty()) {
            return;
        }

        long now = holder.getPipeWorld().getGameTime();

        TravellingItem item = new TravellingItem(toInsert);
        item.side = from;
        item.toCenter = true;
        item.speed = speed;
        item.colour = onInsert.colour;
        item.stack = onInsert.getStack();
        item.genTimings(now, getPipeLength(from));
        item.tried.add(from);
        addItemTryMerge(item);
    }

    private void addItemTryMerge(TravellingItem item) {
        for (List<TravellingItem> list : items.getAllElements()) {
            for (TravellingItem item2 : list) {
                if (item2.mergeWith(item)) {
                    return;
                }
            }
        }
        items.add(item.timeToDest, item);
        sendItemDataToClient(item);
    }

    public boolean doesContainItems() {
        // Note that this counts all items
        // (including phantom items, which is fine)
        // This only works because this list is only expanded to add elements
        // and elements are only removed in advance()
        return items.getMaxDelay() > 0;
    }

    double getPipeLength(Direction side) {
        if (side == null) {
            return 0;
        }
        if (pipe.isConnected(side)) {
            if (pipe.getConnectedType(side) == ConnectedType.TILE) {
                // TODO: Check the length between this pipes centre and the next block along
                return 0.5 + 0.25;// Tiny distance for fully pushing items in.
            }
            return 0.5;
        } else {
            return 0.25;
        }
    }

    public List<TravellingItem> getAllItemsForRender() {
        List<TravellingItem> all = new ArrayList<>();
        for (List<TravellingItem> innerList : items.getAllElements()) {
            all.addAll(innerList);
        }
        return all;
    }
}
