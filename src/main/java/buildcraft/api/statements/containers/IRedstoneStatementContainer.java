/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements.containers;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

/** A statement container that can read and emit redstone signals. */
public interface IRedstoneStatementContainer {
    /** Get the redstone input from a given side.
     *
     * @param side The side - use "null" for maximum input.
     * @return The redstone input, from 0 to 15. */
    int getRedstoneInput(@Nullable Direction side);

    /** Set the redstone output for a given side.
     *
     * @param side The side - use "null" for all sides.
     * @return Whether the set was successful. */
    boolean setRedstoneOutput(@Nullable Direction side, int value);
}
