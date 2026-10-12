/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraftforge.common.ForgeConfigSpec;

/** The energy module's common (server-wide) config. */
public final class BCEnergyConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue OIL_CAN_BURN;
    public static final ForgeConfigSpec.BooleanValue OIL_IS_STICKY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("oil");
        OIL_CAN_BURN = builder
            .comment("Can oil (and the fuels made from it) burn in the world?")
            .define("canBurn", true);
        OIL_IS_STICKY = builder
            .comment("Should oil (and the heavier oils made from it) slow down entities inside it, like cobwebs?")
            .define("isSticky", false);
        builder.pop();

        SPEC = builder.build();
    }

    private BCEnergyConfig() {}

    public static boolean oilCanBurn() {
        return SPEC.isLoaded() ? OIL_CAN_BURN.get() : true;
    }

    public static boolean oilIsSticky() {
        return SPEC.isLoaded() && OIL_IS_STICKY.get();
    }
}
