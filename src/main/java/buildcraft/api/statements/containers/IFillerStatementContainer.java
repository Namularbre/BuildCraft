/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

/** Something that fills a box with a pattern (like a filler). */
public interface IFillerStatementContainer extends IStatementContainer {
    Level getFillerWorld();

    /** @return True if this filler has a box to fill. */
    boolean hasBox();

    /** @return The lowest corner of the box.
     * @throws IllegalStateException if {@link #hasBox()} returns false. */
    BlockPos getBoxMin();

    /** @return The size of the box, in blocks.
     * @throws IllegalStateException if {@link #hasBox()} returns false. */
    BlockPos getBoxSize();

    void setPattern(IFillerPattern pattern, IStatementParameter[] params);
}
