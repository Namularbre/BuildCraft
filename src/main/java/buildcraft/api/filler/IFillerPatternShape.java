/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.filler;

import javax.annotation.Nullable;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;

/** A pattern that only depends on the size of the box. */
public interface IFillerPatternShape extends IFillerPattern {
    /** @param filledTemplate An empty template.
     * @return True if the template was filled, or false if this shouldn't make a template for the given params. */
    boolean fillTemplate(IFilledTemplate filledTemplate, IStatementParameter[] params);

    @Nullable
    @Override
    default IFilledTemplate createTemplate(IFillerStatementContainer filler, IStatementParameter[] params) {
        IFilledTemplate template = FillerManager.registry.createFilledTemplate(filler.getBoxMin(), filler.getBoxSize());
        return fillTemplate(template, params) ? template : null;
    }
}
