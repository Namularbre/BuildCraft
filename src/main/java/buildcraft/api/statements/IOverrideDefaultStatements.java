/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements;

import java.util.List;

/** A block entity implementing this replaces the external statements that every provider would give it. */
public interface IOverrideDefaultStatements {
    /** @return The triggers to use instead, or null to use the normal ones. */
    List<ITriggerExternal> overrideTriggers();

    /** @return The actions to use instead, or null to use the normal ones. */
    List<IActionExternal> overrideActions();
}
