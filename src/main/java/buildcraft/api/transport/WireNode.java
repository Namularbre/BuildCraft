/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;

/** A wire corner, in a given pipe. */
public record WireNode(BlockPos pos, EnumWirePart part) {
    /** @return The corner next to this one in the given direction: in the same pipe, or in the next one. */
    public WireNode offset(Direction face) {
        int nx = (part.x == AxisDirection.POSITIVE ? 1 : 0) + face.getStepX();
        int ny = (part.y == AxisDirection.POSITIVE ? 1 : 0) + face.getStepY();
        int nz = (part.z == AxisDirection.POSITIVE ? 1 : 0) + face.getStepZ();
        EnumWirePart nPart = EnumWirePart.get(nx, ny, nz);
        if (nx < 0 || ny < 0 || nz < 0 || nx > 1 || ny > 1 || nz > 1) {
            return new WireNode(pos.relative(face), nPart);
        } else {
            return new WireNode(pos, nPart);
        }
    }

    @Override
    public String toString() {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + part + ")";
    }
}
