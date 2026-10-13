/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.Locale;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** What a gate is made of: this decides how many trigger/action pairs it has. */
public enum EnumGateMaterial {
    CLAY_BRICK(Blocks.BRICKS, "bricks", 1, false),
    IRON(Blocks.IRON_BLOCK, "iron_block", 2, true),
    NETHER_BRICK(Blocks.NETHER_BRICKS, "nether_bricks", 4, true),
    GOLD(Blocks.GOLD_BLOCK, "gold_block", 8, true);

    public static final EnumGateMaterial[] VALUES = values();

    public final Block block;
    /** The texture of the gate (in the vanilla block textures). */
    public final String texture;
    public final int numSlots;
    public final boolean canBeModified;
    public final String tag = name().toLowerCase(Locale.ROOT);

    EnumGateMaterial(Block block, String texture, int numSlots, boolean canBeModified) {
        this.block = block;
        this.texture = texture;
        this.numSlots = numSlots;
        this.canBeModified = canBeModified;
    }

    public static EnumGateMaterial getByOrdinal(int ord) {
        if (ord < 0 || ord >= VALUES.length) {
            return CLAY_BRICK;
        }
        return VALUES[ord];
    }
}
