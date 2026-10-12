/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.blocks;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.registries.ForgeRegistries;

/** Lets the BuildCraft wrench rotate blocks. Blocks can either implement {@link ICustomRotationHandler} themselves, or
 * have a handler registered here. */
public enum CustomRotationHelper {
    INSTANCE;

    private final Map<Block, List<ICustomRotationHandler>> handlers = new IdentityHashMap<>();

    /** Registers the handler for every registered block that is an instance of the given class. Must be called after
     * blocks have been registered. */
    public void registerHandlerForAll(Class<? extends Block> blockClass, ICustomRotationHandler handler) {
        for (Block block : ForgeRegistries.BLOCKS) {
            if (blockClass.isInstance(block)) {
                registerHandler(block, handler);
            }
        }
    }

    public void registerHandler(Block block, ICustomRotationHandler handler) {
        handlers.computeIfAbsent(block, b -> new ArrayList<>()).add(handler);
    }

    public InteractionResult attemptRotateBlock(Level level, BlockPos pos, BlockState state, Direction sideWrenched) {
        Block block = state.getBlock();
        if (block instanceof ICustomRotationHandler) {
            return ((ICustomRotationHandler) block).attemptRotation(level, pos, state, sideWrenched);
        }
        List<ICustomRotationHandler> forBlock = handlers.get(block);
        if (forBlock == null) return InteractionResult.PASS;
        for (ICustomRotationHandler handler : forBlock) {
            InteractionResult result = handler.attemptRotation(level, pos, state, sideWrenched);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }
}
