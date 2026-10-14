/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.IStripesActivator;

/** Moves stripes pipes forwards (or backwards) when they are given pipes. */
public interface IPipeExtensionManager {
    /** Requests an extension by one block from a {@link IStripesActivator} (usually a stripes transport pipe) with the
     * pipe supplied by the stack, by moving the stripes pipe to the front and placing the new transport pipe behind.
     * If the pipe is a registered retraction pipe (by default only the void transport pipe is - register one with
     * {@link #registerRetractionPipe(PipeDefinition)}) it retracts the pipeline instead by moving the stripes pipe one
     * block in the opposite direction, replacing the previous transport pipe.
     *
     * @param pos The position of the stripes pipe.
     * @param dir The direction of the proposed extension.
     * @param stack The pipe stack to use (only one item is used: the others are sent back).
     * @return true on success, false otherwise */
    boolean requestPipeExtension(Level level, BlockPos pos, Direction dir, IStripesActivator stripes,
        ItemStack stack);

    /** Registers a pipe as a retraction trigger for pipe extension requests. */
    void registerRetractionPipe(PipeDefinition pipeDefinition);
}
