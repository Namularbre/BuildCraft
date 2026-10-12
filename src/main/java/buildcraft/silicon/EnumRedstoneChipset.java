/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.Locale;

/** The chipsets made in the assembly table, used in gates. */
public enum EnumRedstoneChipset {
    RED,
    IRON,
    GOLD,
    QUARTZ,
    DIAMOND;

    /** The name of the item: "redstone_chipset" for the red one, "iron_chipset" for the others. */
    public String getItemName() {
        return this == RED ? "redstone_chipset" : name().toLowerCase(Locale.ROOT) + "_chipset";
    }

    /** The name of the texture (and of the 1.12 variant). */
    public String getTextureName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
