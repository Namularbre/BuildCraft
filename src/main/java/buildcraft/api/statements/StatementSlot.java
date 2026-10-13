/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements;

import java.util.Arrays;
import java.util.Objects;

import buildcraft.api.core.EnumPipePart;

/** An active statement, with its parameters. */
public class StatementSlot {
    public IStatement statement;
    public IStatementParameter[] parameters;
    public EnumPipePart part = EnumPipePart.CENTER;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StatementSlot s)) {
            return false;
        }
        return s.statement == statement && Arrays.equals(parameters, s.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statement, Arrays.deepHashCode(parameters));
    }
}
