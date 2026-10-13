/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.IWireEmitter;
import buildcraft.api.transport.WireNode;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.PipePluggable;

/** The wire systems of a (server) level: every group of wires of the same colour that are joined together. They are
 * found again when they are needed, after anything that could change them (wires, pipes, connections or pluggables).
 * <p>
 * A system is powered if any {@link IWireEmitter} pluggable, on any pipe that the system goes through, emits its
 * colour. */
public final class WireSystems {
    private static final Map<Level, WireSystems> LEVELS = new WeakHashMap<>();

    private final Level level;
    private final Map<WireNode, WireSystem> systems = new HashMap<>();

    private WireSystems(Level level) {
        this.level = level;
    }

    public static WireSystems get(Level level) {
        return LEVELS.computeIfAbsent(level, WireSystems::new);
    }

    /** Forgets every system of the given level, so that they are found again when they are needed. */
    public static void invalidate(@Nullable Level level) {
        if (level != null && !level.isClientSide) {
            WireSystems systems = LEVELS.get(level);
            if (systems != null) {
                systems.systems.clear();
            }
        }
    }

    public boolean isPowered(WireNode node) {
        WireSystem system = systems.get(node);
        if (system == null) {
            system = build(node);
            if (system == null) {
                return false;
            }
        }
        return system.isPowered();
    }

    @Nullable
    private WireSystem build(WireNode start) {
        IPipeHolder startHolder = getHolder(start.pos());
        DyeColor colour = startHolder == null ? null : startHolder.getWireManager().getColorOfPart(start.part());
        if (colour == null) {
            return null;
        }
        WireSystem system = new WireSystem(colour);
        Set<WireNode> walked = new HashSet<>();
        Queue<WireNode> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            WireNode node = queue.remove();
            if (!walked.add(node)) {
                continue;
            }
            IPipeHolder holder = getHolder(node.pos());
            if (holder == null || holder.getWireManager().getColorOfPart(node.part()) != colour) {
                continue;
            }
            system.pipes.add(node.pos());
            systems.put(node, system);
            for (Direction face : Direction.values()) {
                WireNode next = node.offset(face);
                if (next.pos().equals(node.pos()) || canWireConnect(holder, face)) {
                    queue.add(next);
                }
            }
        }
        return system;
    }

    @Nullable
    private IPipeHolder getHolder(BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof IPipeHolder holder ? holder : null;
    }

    /** Checks to see if the given holder could connect a wire across the specified side even if a matching wire
     * wasn't there. */
    public static boolean canWireConnect(IPipeHolder holder, Direction side) {
        IPipe pipe = holder.getPipe();
        if (pipe == null) {
            return false;
        }
        IPipe oPipe = holder.getNeighbourPipe(side);
        if (oPipe == null) {
            return false;
        }
        if (pipe.isConnected(side)) {
            return true;
        }
        PipePluggable plug = holder.getPluggable(side);
        PipePluggable oPlug = oPipe.getHolder().getPluggable(side.getOpposite());
        if ((plug != null && plug.isBlocking()) || (oPlug != null && oPlug.isBlocking())) {
            return false;
        }
        if (pipe.getDefinition().flowType == PipeApi.flowStructure
            || oPipe.getDefinition().flowType == PipeApi.flowStructure) {
            return pipe.getColour() == null || oPipe.getColour() == null || pipe.getColour() == oPipe.getColour();
        }
        return false;
    }

    private final class WireSystem {
        final DyeColor colour;
        /** Every pipe that this goes through: the emitters on any of them power this. */
        final Set<BlockPos> pipes = new HashSet<>();
        private long lastTick = Long.MIN_VALUE;
        private boolean powered;

        WireSystem(DyeColor colour) {
            this.colour = colour;
        }

        /** Only looks at the emitters once per tick. */
        boolean isPowered() {
            long now = level.getGameTime();
            if (now != lastTick) {
                lastTick = now;
                powered = findEmitter();
            }
            return powered;
        }

        private boolean findEmitter() {
            for (BlockPos pos : pipes) {
                IPipeHolder holder = getHolder(pos);
                if (holder == null) {
                    continue;
                }
                for (Direction side : Direction.values()) {
                    if (holder.getPluggable(side) instanceof IWireEmitter emitter && emitter.isEmitting(colour)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
