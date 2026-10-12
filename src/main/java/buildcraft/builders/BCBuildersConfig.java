/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraftforge.common.ForgeConfigSpec;

/** The builders module's common (server-wide) config. */
public final class BCBuildersConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue QUARRY_FRAME_MIN_HEIGHT;
    public static final ForgeConfigSpec.IntValue QUARRY_MAX_TASKS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue QUARRY_MAX_FRAME_MOVE_SPEED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("quarry");
        QUARRY_FRAME_MIN_HEIGHT = builder
            .comment("The minimum height of quarry frames. A value of 1 looks strange when it drills the top layer.")
            .defineInRange("frameMinHeight", 4, 1, 64);
        QUARRY_MAX_TASKS_PER_TICK = builder
            .comment("How many things (breaking a block, placing frame...) a quarry can do each tick, at most.")
            .defineInRange("maxTasksPerTick", 4, 1, 64);
        QUARRY_MAX_FRAME_MOVE_SPEED = builder
            .comment("The fastest the drill can move, in blocks per second (0 for no limit).")
            .defineInRange("maxFrameMoveSpeed", 0.0, 0.0, 1000.0);
        builder.pop();
        SPEC = builder.build();
    }

    private BCBuildersConfig() {}

    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int quarryFrameMinHeight() {
        return get(QUARRY_FRAME_MIN_HEIGHT);
    }

    public static int quarryMaxTasksPerTick() {
        return get(QUARRY_MAX_TASKS_PER_TICK);
    }

    public static double quarryMaxFrameMoveSpeed() {
        return get(QUARRY_MAX_FRAME_MOVE_SPEED);
    }
}
