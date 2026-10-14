/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeExtensionManager;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.tile.TilePipeHolder;

/** Moves stripes pipes: forwards when they are given an item pipe (which is placed behind them), and backwards when
 * they are given a retraction pipe (which takes the place of the pipe behind them). The moves are done at the end of
 * the tick, so that the pipe isn't moved while it ticks. */
public enum PipeExtensionManager implements IPipeExtensionManager {
    INSTANCE;

    private final Map<Level, List<Request>> requests = new WeakHashMap<>();
    private final Set<PipeDefinition> retractionPipeDefs = new HashSet<>();

    private record Request(BlockPos pos, Direction dir, PipeDefinition pipeDef, ItemStack stack) {}

    @Override
    public boolean requestPipeExtension(Level level, BlockPos pos, Direction dir, IStripesActivator stripes,
        ItemStack stack) {
        if (level.isClientSide || stack.isEmpty() || !(stack.getItem() instanceof IItemPipe pipe)) {
            return false;
        }
        requests.computeIfAbsent(level, l -> new ArrayList<>())
            .add(new Request(pos, dir, pipe.getDefinition(), stack.copy()));
        return true;
    }

    @Override
    public void registerRetractionPipe(PipeDefinition pipeDefinition) {
        if (pipeDefinition != null) {
            retractionPipeDefs.add(pipeDefinition);
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        List<Request> list = requests.remove(level);
        if (list == null) {
            return;
        }
        for (Request r : list) {
            if (retractionPipeDefs.contains(r.pipeDef)) {
                retract(level, r);
            } else {
                extend(level, r);
            }
        }
    }

    private void extend(ServerLevel level, Request r) {
        BlockPos to = r.pos.relative(r.dir);
        if (!(level.getBlockEntity(r.pos) instanceof TilePipeHolder stripes)) {
            return;
        }
        List<ItemStack> sendBack = new ArrayList<>();
        ItemStack pipeStack = r.stack.copy();
        ItemStack placed = pipeStack.split(1);
        sendBack.add(pipeStack);
        if (!level.getBlockState(to).canBeReplaced() || !canPlace(level, to, stripes.getOwner(), r.dir)) {
            sendBack.add(placed);
            sendItems(level, r.pos, r.dir, sendBack);
            return;
        }
        // Move the stripes pipe forwards, and put the new pipe where it was
        CompoundTag stripesData = stripes.saveWithoutMetadata();
        BlockState stripesState = level.getBlockState(r.pos);
        // Both are pipes, so only the block entity changes
        level.removeBlockEntity(r.pos);
        TilePipeHolder newPipe = new TilePipeHolder(r.pos, stripesState);
        level.setBlockEntity(newPipe);
        newPipe.onPlacedBy(FakePlayerFactory.get(level, stripes.getOwner()), placed);
        placeStripes(level, to, stripesState, stripesData);
        sendItems(level, to, r.dir, sendBack);
    }

    private void retract(ServerLevel level, Request r) {
        if (!(level.getBlockEntity(r.pos) instanceof TilePipeHolder stripes)) {
            return;
        }
        List<ItemStack> sendBack = new ArrayList<>();
        // The retraction pipe is always sent back
        sendBack.add(r.stack.copy());
        Direction retractDir = r.dir.getOpposite();
        if (!isValidRetractionPath(level, r.pos, retractDir)) {
            List<Direction> possible = new ArrayList<>();
            for (Direction face : Direction.values()) {
                if (face.getAxis() != r.dir.getAxis() && isValidRetractionPath(level, r.pos, face)) {
                    possible.add(face);
                }
            }
            if (possible.isEmpty()) {
                sendItems(level, r.pos, r.dir, sendBack);
                return;
            }
            retractDir = possible.get(level.random.nextInt(possible.size()));
        }
        BlockPos to = r.pos.relative(retractDir);
        if (!(level.getBlockEntity(to) instanceof TilePipeHolder previous)
            || !canBreak(level, to, stripes.getOwner())) {
            sendItems(level, r.pos, r.dir, sendBack);
            return;
        }
        // Take the pipe behind (and what it holds), and move the stripes pipe there
        NonNullList<ItemStack> drops = NonNullList.create();
        previous.addDrops(drops);
        sendBack.addAll(drops);
        CompoundTag stripesData = stripes.saveWithoutMetadata();
        BlockState stripesState = level.getBlockState(r.pos);
        level.removeBlockEntity(to);
        level.removeBlock(to, false);
        level.removeBlockEntity(r.pos);
        level.removeBlock(r.pos, false);
        placeStripes(level, to, stripesState, stripesData);
        sendItems(level, to, retractDir.getOpposite(), sendBack);
    }

    private static void placeStripes(ServerLevel level, BlockPos pos, BlockState state, CompoundTag data) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile) {
            tile.load(data);
            tile.onLoad();
            tile.setChanged();
            tile.sendNetworkUpdate(TilePipeHolder.NET_RENDER_DATA);
        }
    }

    /** Gives the items to the stripes pipe at the given position (or drops them if it isn't there). */
    private static void sendItems(ServerLevel level, BlockPos pos, Direction dir, List<ItemStack> stacks) {
        IStripesActivator stripes = null;
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile && tile.getPipe() != null
            && tile.getPipe().getBehaviour() instanceof IStripesActivator activator) {
            stripes = activator;
        }
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stripes == null || !stripes.sendItem(stack.copy(), dir)) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    /** @return True if no other mod (like a protection mod) stops the pipe from being placed there. */
    private static boolean canPlace(ServerLevel level, BlockPos pos, GameProfile owner, Direction dir) {
        FakePlayer player = FakePlayerFactory.get(level, owner);
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        BlockState placed = BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState();
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot,
            level.getBlockState(pos.relative(dir.getOpposite())), player) {
            @Override
            public BlockState getPlacedBlock() {
                return placed;
            }
        };
        return !MinecraftForge.EVENT_BUS.post(event);
    }

    private static boolean canBreak(ServerLevel level, BlockPos pos, GameProfile owner) {
        FakePlayer player = FakePlayerFactory.get(level, owner);
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player));
    }

    /** @return True if the pipe on the given side only goes to the stripes pipe and one other pipe (so it can be
     *         taken away without breaking the line). */
    private static boolean isValidRetractionPath(ServerLevel level, BlockPos pos, Direction retractDir) {
        if (!(level.getBlockEntity(pos.relative(retractDir)) instanceof TilePipeHolder tile)) {
            return false;
        }
        IPipe pipe = tile.getPipe();
        if (pipe == null) {
            return false;
        }
        boolean connected = false;
        for (Direction face : Direction.values()) {
            IPipe.ConnectedType type = pipe.getConnectedType(face);
            if (type == IPipe.ConnectedType.TILE) {
                return false;
            }
            if (face == retractDir.getOpposite()) {
                if (type != IPipe.ConnectedType.PIPE) {
                    return false;
                }
            } else if (type != null) {
                if (connected) {
                    return false;
                }
                connected = true;
            }
        }
        return true;
    }
}
