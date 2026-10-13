/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import buildcraft.BuildCraft;

/** The textures of the filler patterns and their parameters (whole pngs, drawn in GUIs). */
public final class FillerSprites {
    private FillerSprites() {}

    public static ResourceLocation pattern(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/filler/patterns/" + name + ".png");
    }

    public static ResourceLocation param(String name) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID,
            "textures/gui/filler/parameters/" + name + ".png");
    }

    public static ResourceLocation xzDir(Direction dir) {
        return param(switch (dir) {
            case WEST -> "arrow_left";
            case EAST -> "arrow_right";
            case NORTH -> "arrow_up";
            default -> "arrow_down";
        });
    }
}
