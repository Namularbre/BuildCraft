/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.transport;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;

import buildcraft.api.transport.pipe.IPipeHolder;

/** The wires that go through a pipe. */
public interface IWireManager {
    IPipeHolder getHolder();

    void updateBetweens(boolean recursive);

    @Nullable
    DyeColor getColorOfPart(EnumWirePart part);

    /** @return The colour of the removed wire, or null if there wasn't one. */
    @Nullable
    DyeColor removePart(EnumWirePart part);

    /** @return True if the wire was added, false if there already was one there. */
    boolean addPart(EnumWirePart part, DyeColor colour);

    boolean hasPartOfColor(DyeColor color);

    boolean isPowered(EnumWirePart part);

    boolean isAnyPowered(DyeColor color);
}
