/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.filler;

import javax.annotation.Nullable;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;

/** A type of statement that is used for filler patterns. */
public interface IFillerPattern extends IStatement {
    /** @param filler The filler to create the pattern for. This is never called when
     *            {@link IFillerStatementContainer#hasBox()} returns false.
     * @return The template to fill (created with {@link IFillerRegistry#createFilledTemplate}), or null if this
     *         shouldn't make a template for the given filler. */
    @Nullable
    IFilledTemplate createTemplate(IFillerStatementContainer filler, IStatementParameter[] params);

    @Override
    IFillerPattern[] getPossible();
}
