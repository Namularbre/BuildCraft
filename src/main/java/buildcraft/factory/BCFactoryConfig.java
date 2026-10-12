/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.minecraftforge.common.ForgeConfigSpec;

/** The factory module's common (server-wide) config. */
public final class BCFactoryConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MINING_MAX_DEPTH;
    public static final ForgeConfigSpec.DoubleValue MINING_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue PUMP_MAX_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue PUMPS_CONSUME_WATER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("mining");
        MINING_MAX_DEPTH = builder
            .comment("How deep (in blocks) pumps and mining wells can reach.")
            .defineInRange("miningMaxDepth", 512, 1, 4096);
        MINING_MULTIPLIER = builder
            .comment("How much more power mining wells need to break blocks.")
            .defineInRange("miningMultiplier", 1.0, 1.0, 200.0);
        PUMP_MAX_DISTANCE = builder
            .comment("How far (in blocks) from the end of its tube a pump will pump fluids.")
            .defineInRange("pumpMaxDistance", 64, 1, 1024);
        PUMPS_CONSUME_WATER = builder
            .comment("If false, pumps don't remove water from infinite water sources (like oceans).")
            .define("pumpsConsumeWater", false);
        builder.pop();

        SPEC = builder.build();
    }

    private BCFactoryConfig() {}

    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int miningMaxDepth() {
        return get(MINING_MAX_DEPTH);
    }

    public static double miningMultiplier() {
        return get(MINING_MULTIPLIER);
    }

    public static int pumpMaxDistance() {
        return get(PUMP_MAX_DISTANCE);
    }

    public static boolean pumpsConsumeWater() {
        return get(PUMPS_CONSUME_WATER);
    }
}
