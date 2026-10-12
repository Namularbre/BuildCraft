/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface ICustomRotationHandler {
    /** @return {@link InteractionResult#SUCCESS} if the block was rotated, {@link InteractionResult#FAIL} if it
     *         couldn't be, or {@link InteractionResult#PASS} if this handler doesn't know how to rotate it. */
    InteractionResult attemptRotation(Level level, BlockPos pos, BlockState state, Direction sideWrenched);
}
