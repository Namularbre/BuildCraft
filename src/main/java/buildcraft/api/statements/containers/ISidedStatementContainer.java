/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements.containers;

import net.minecraft.core.Direction;

import buildcraft.api.statements.IStatementContainer;

/** A statement container that is on one side of something (like a gate on a pipe). */
public interface ISidedStatementContainer extends IStatementContainer {
    Direction getSide();
}
