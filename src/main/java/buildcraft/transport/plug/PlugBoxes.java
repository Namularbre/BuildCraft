/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.plug;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** The boxes of simple pluggables: a square plate on a side of the pipe. */
final class PlugBoxes {
    private PlugBoxes() {}

    /** @param from How far (in pixels) from the side of the block the plate starts.
     * @param inset How far (in pixels) from the edges of the side the plate is.
     * @param to How far (in pixels) from the side of the block the plate ends.
     * @return The box for each side (indexed by {@link Direction#ordinal()}). */
    static AABB[] make(double from, double inset, double to) {
        double ll = from / 16, lu = to / 16, ul = 1 - to / 16, uu = 1 - from / 16;
        double min = inset / 16, max = 1 - inset / 16;
        AABB[] boxes = new AABB[6];
        boxes[Direction.DOWN.ordinal()] = new AABB(min, ll, min, max, lu, max);
        boxes[Direction.UP.ordinal()] = new AABB(min, ul, min, max, uu, max);
        boxes[Direction.NORTH.ordinal()] = new AABB(min, min, ll, max, max, lu);
        boxes[Direction.SOUTH.ordinal()] = new AABB(min, min, ul, max, max, uu);
        boxes[Direction.WEST.ordinal()] = new AABB(ll, min, min, lu, max, max);
        boxes[Direction.EAST.ordinal()] = new AABB(ul, min, min, uu, max, max);
        return boxes;
    }
}
