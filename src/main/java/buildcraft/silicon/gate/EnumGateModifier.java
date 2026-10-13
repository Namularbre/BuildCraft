/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.Locale;

/** What a gate is upgraded with: this gives parameters to its statements (but halves its slots). */
public enum EnumGateModifier {
    NO_MODIFIER(0, 0, 1, null),
    LAPIS(1, 0, 1, "lapis_block"),
    QUARTZ(1, 1, 2, "quartz_block_top"),
    DIAMOND(3, 3, 2, "diamond_block");

    public static final EnumGateModifier[] VALUES = values();

    public final int triggerParams, actionParams;
    public final int slotDivisor;
    /** The texture of the modifier (in the vanilla block textures), or null if it isn't drawn. */
    public final String texture;
    public final String tag = name().toLowerCase(Locale.ROOT);

    EnumGateModifier(int triggerParams, int actionParams, int slotDivisor, String texture) {
        this.triggerParams = triggerParams;
        this.actionParams = actionParams;
        this.slotDivisor = slotDivisor;
        this.texture = texture;
    }

    public static EnumGateModifier getByOrdinal(int ord) {
        if (ord < 0 || ord >= VALUES.length) {
            return NO_MODIFIER;
        }
        return VALUES[ord];
    }
}
