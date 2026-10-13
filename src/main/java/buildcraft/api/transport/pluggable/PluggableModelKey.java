/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.transport.pluggable;

import java.util.Objects;

import net.minecraft.core.Direction;

/** Identifies what a pluggable looks like, so that its model can be cached. Two keys of the same class on the same
 * side are equal: subclasses with more data must override {@link #equals(Object)} and {@link #hashCode()}. */
public abstract class PluggableModelKey {
    public final Direction side;

    public PluggableModelKey(Direction side) {
        this.side = Objects.requireNonNull(side, "side");
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj != null && obj.getClass() == getClass() && ((PluggableModelKey) obj).side == side);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), side);
    }
}
