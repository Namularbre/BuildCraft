/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import buildcraft.energy.BCEnergyConfig;

/** The oil fields: the parts of oceans and deserts where 1.12 placed its oil ocean and oil desert biomes. Those biomes
 * aren't ported, so the same noise decides where the fields are, without changing the biome. Oil is much more common
 * in them. */
public final class OilFields {
    // The noise of the 1.12 biome layers (in block coordinates)
    private static final double OCEAN_NOISE_SCALE = 0.0005;
    private static final double OCEAN_NOISE_THRESHOLD = 0.9;
    private static final double DESERT_NOISE_SCALE = 0.001;
    private static final double DESERT_NOISE_THRESHOLD = 0.7;
    /** Different from the other generators, so that the fields don't follow other noises. */
    private static final long MAGIC_FIELD_NUMBER = 0x6F_69_6C_46_69_65_6C_64L;

    private static long cachedSeed;
    private static SimplexNoise cachedNoise;

    private OilFields() {}

    private static synchronized SimplexNoise getNoise(long worldSeed) {
        if (cachedNoise == null || cachedSeed != worldSeed) {
            cachedSeed = worldSeed;
            cachedNoise = new SimplexNoise(new LegacyRandomSource(worldSeed ^ MAGIC_FIELD_NUMBER));
        }
        return cachedNoise;
    }

    /** @return True if the given column is in an oil field. */
    public static boolean isInField(long worldSeed, int x, int z, ResourceLocation biome) {
        if (!BCEnergyConfig.enableOilFields()) {
            return false;
        }
        String name = biome.toString();
        if (BCEnergyConfig.oilOceanBiomes().contains(name)) {
            return getNoise(worldSeed).getValue(x * OCEAN_NOISE_SCALE, z * OCEAN_NOISE_SCALE) > OCEAN_NOISE_THRESHOLD;
        }
        if (BCEnergyConfig.oilDesertBiomes().contains(name)) {
            // A different part of the noise than the oceans
            return getNoise(worldSeed).getValue(x * DESERT_NOISE_SCALE + 1000, z * DESERT_NOISE_SCALE + 1000)
                > DESERT_NOISE_THRESHOLD;
        }
        return false;
    }
}
