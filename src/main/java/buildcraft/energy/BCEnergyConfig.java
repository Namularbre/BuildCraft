/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

/** The energy module's common (server-wide) config. */
public final class BCEnergyConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue OIL_CAN_BURN;
    public static final ForgeConfigSpec.BooleanValue OIL_IS_STICKY;

    public static final ForgeConfigSpec.BooleanValue ENABLE_OIL_GENERATION;
    public static final ForgeConfigSpec.DoubleValue OIL_WELL_GENERATION_RATE;
    public static final ForgeConfigSpec.DoubleValue SMALL_OIL_GEN_PROB;
    public static final ForgeConfigSpec.DoubleValue MEDIUM_OIL_GEN_PROB;
    public static final ForgeConfigSpec.DoubleValue LARGE_OIL_GEN_PROB;
    public static final ForgeConfigSpec.BooleanValue ENABLE_OIL_SPOUTS;
    public static final ForgeConfigSpec.IntValue SMALL_SPOUT_MIN_HEIGHT;
    public static final ForgeConfigSpec.IntValue SMALL_SPOUT_MAX_HEIGHT;
    public static final ForgeConfigSpec.IntValue LARGE_SPOUT_MIN_HEIGHT;
    public static final ForgeConfigSpec.IntValue LARGE_SPOUT_MAX_HEIGHT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCESSIVE_BIOMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SURFACE_DEPOSIT_BIOMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_BIOMES;
    public static final ForgeConfigSpec.BooleanValue EXCLUDED_BIOMES_IS_BLACKLIST;

    /** 1.12 had dedicated oil desert and oil ocean biomes, which aren't ported: oil is more common in deserts and
     * oceans instead. */
    private static final List<String> DEFAULT_SURFACE_DEPOSIT_BIOMES = List.of(
        "minecraft:desert", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:warm_ocean",
        "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"
    );

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

        builder.push("worldgen");
        ENABLE_OIL_GENERATION = builder
            .comment("Should any oil spouts or lakes be generated at all?")
            .define("enable", true);
        OIL_WELL_GENERATION_RATE = builder
            .comment("The rate of occurrence of oil wells.")
            .defineInRange("generationRate", 1.0, 0, 1000);
        SMALL_OIL_GEN_PROB = builder
            .comment("The percentage probability of a small oil lake (only in surface deposit biomes).")
            .defineInRange("smallProbability", 2.0, 0, 100);
        MEDIUM_OIL_GEN_PROB = builder
            .comment("The percentage probability of a medium oil well.")
            .defineInRange("mediumProbability", 0.1, 0, 100);
        LARGE_OIL_GEN_PROB = builder
            .comment("The percentage probability of a large oil well.")
            .defineInRange("largeProbability", 0.04, 0, 100);
        ENABLE_OIL_SPOUTS = builder
            .comment("Whether oil spouts are generated above oil wells or not.")
            .define("enableSpouts", true);
        SMALL_SPOUT_MIN_HEIGHT = builder.defineInRange("smallSpoutMinHeight", 6, 0, 256);
        SMALL_SPOUT_MAX_HEIGHT = builder.defineInRange("smallSpoutMaxHeight", 12, 0, 256);
        LARGE_SPOUT_MIN_HEIGHT = builder.defineInRange("largeSpoutMinHeight", 10, 0, 256);
        LARGE_SPOUT_MAX_HEIGHT = builder.defineInRange("largeSpoutMaxHeight", 20, 0, 256);
        EXCESSIVE_BIOMES = builder
            .comment("Biome registry names (e.g. 'minecraft:ocean') of biomes that should have GREATLY increased oil "
                + "generation rates.")
            .defineListAllowEmpty("excessiveBiomes", List.of(), o -> o instanceof String);
        SURFACE_DEPOSIT_BIOMES = builder
            .comment("Biome registry names of biomes that should have slightly increased oil generation rates, and "
                + "oil lakes.")
            .defineListAllowEmpty("surfaceDepositBiomes", DEFAULT_SURFACE_DEPOSIT_BIOMES, o -> o instanceof String);
        EXCLUDED_BIOMES = builder
            .comment("Biome registry names of biomes that should never generate oil.")
            .defineListAllowEmpty("excludedBiomes", List.of(), o -> o instanceof String);
        EXCLUDED_BIOMES_IS_BLACKLIST = builder
            .comment("If true then the excluded biomes list is a blacklist, otherwise it is a whitelist.")
            .define("excludedBiomesIsBlacklist", true);
        builder.pop();

        SPEC = builder.build();
    }

    private BCEnergyConfig() {}

    private static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean oilCanBurn() {
        return get(OIL_CAN_BURN);
    }

    public static boolean oilIsSticky() {
        return get(OIL_IS_STICKY);
    }

    public static boolean enableOilGeneration() {
        return get(ENABLE_OIL_GENERATION);
    }

    public static double oilWellGenerationRate() {
        return get(OIL_WELL_GENERATION_RATE);
    }

    /** @return The probability (from 0 to 1) of a small oil lake. */
    public static double smallOilGenProb() {
        return get(SMALL_OIL_GEN_PROB) / 100;
    }

    public static double mediumOilGenProb() {
        return get(MEDIUM_OIL_GEN_PROB) / 100;
    }

    public static double largeOilGenProb() {
        return get(LARGE_OIL_GEN_PROB) / 100;
    }

    public static boolean enableOilSpouts() {
        return get(ENABLE_OIL_SPOUTS);
    }

    public static int smallSpoutMinHeight() {
        return get(SMALL_SPOUT_MIN_HEIGHT);
    }

    public static int smallSpoutMaxHeight() {
        return get(SMALL_SPOUT_MAX_HEIGHT);
    }

    public static int largeSpoutMinHeight() {
        return get(LARGE_SPOUT_MIN_HEIGHT);
    }

    public static int largeSpoutMaxHeight() {
        return get(LARGE_SPOUT_MAX_HEIGHT);
    }

    public static List<? extends String> excessiveBiomes() {
        return get(EXCESSIVE_BIOMES);
    }

    public static List<? extends String> surfaceDepositBiomes() {
        return get(SURFACE_DEPOSIT_BIOMES);
    }

    public static List<? extends String> excludedBiomes() {
        return get(EXCLUDED_BIOMES);
    }

    public static boolean excludedBiomesIsBlacklist() {
        return get(EXCLUDED_BIOMES_IS_BLACKLIST);
    }
}
