/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements;

/** A block entity that wants to know when an external action is activated on it. */
public interface IActionReceptor {
    void actionActivated(IStatement statement, IStatementParameter[] parameters);
}
