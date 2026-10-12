/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import buildcraft.energy.BCEnergyConfig;

/** Works out the oil deposits that a chunk holds. This only depends on the world seed and the chunk position, so
 * generating any chunk can find the parts of the deposits of nearby chunks that reach into it. */
public final class OilGenerator {
    /** Random number, used to differentiate generators */
    private static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;

    /** The distance (in chunks) that oil deposits are looked for around a generating chunk. All oil generation must fit
     * inside this radius. */
    public static final int MAX_CHUNK_RADIUS = 5;

    private OilGenerator() {}

    public enum GenType {
        LARGE,
        MEDIUM,
        LAKE,
        NONE
    }

    /** Looks up the biome of a column, which may be in a chunk that hasn't generated yet. */
    @FunctionalInterface
    public interface BiomeLookup {
        ResourceLocation getBiome(int x, int z);
    }

    /** @return The same random numbers as 1.12 for the given chunk. */
    static Random createRandomForChunk(long worldSeed, int chunkX, int chunkZ) {
        Random worldRandom = new Random(worldSeed);
        // Note that this is ">> 3" (1.12 had the same precedence)
        long xSeed = worldRandom.nextLong() >> 2 + 1L;
        long zSeed = worldRandom.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
        // XOR our own number so that we differ from other generators
        chunkSeed ^= MAGIC_GEN_NUMBER;
        return new Random(chunkSeed);
    }

    /** @return The type of deposit centred in the given chunk, without working out its shape. */
    public static GenType getType(long worldSeed, int cx, int cz, BiomeLookup biomes) {
        return roll(createRandomForChunk(worldSeed, cx, cz), cx, cz, biomes).type;
    }

    private record Roll(GenType type, int x, int z) {}

    private static Roll roll(Random rand, int cx, int cz, BiomeLookup biomes) {
        int x = cx * 16 + 8 + rand.nextInt(16);
        int z = cz * 16 + 8 + rand.nextInt(16);

        ResourceLocation biome = biomes.getBiome(x, z);
        if (biome == null) {
            return new Roll(GenType.NONE, x, z);
        }
        // Do not generate oil in excluded biomes
        boolean isExcludedBiome = BCEnergyConfig.excludedBiomes().contains(biome.toString());
        if (isExcludedBiome == BCEnergyConfig.excludedBiomesIsBlacklist()) {
            return new Roll(GenType.NONE, x, z);
        }

        boolean oilBiome = BCEnergyConfig.surfaceDepositBiomes().contains(biome.toString());

        double bonus = oilBiome ? 3.0 : 1.0;
        bonus *= BCEnergyConfig.oilWellGenerationRate();
        if (BCEnergyConfig.excessiveBiomes().contains(biome.toString())) {
            bonus *= 30.0;
        }

        final GenType type;
        if (rand.nextDouble() <= BCEnergyConfig.largeOilGenProb() * bonus) {
            type = GenType.LARGE;
        } else if (rand.nextDouble() <= BCEnergyConfig.mediumOilGenProb() * bonus) {
            type = GenType.MEDIUM;
        } else if (oilBiome && rand.nextDouble() <= BCEnergyConfig.smallOilGenProb() * bonus) {
            type = GenType.LAKE;
        } else {
            type = GenType.NONE;
        }
        return new Roll(type, x, z);
    }

    /** @param maxY The top of the world. */
    public static List<OilGenStructure> getStructures(long worldSeed, int cx, int cz, BiomeLookup biomes, int minY,
        int maxY) {
        Random rand = createRandomForChunk(worldSeed, cx, cz);
        Roll roll = roll(rand, cx, cz, biomes);
        if (roll.type == GenType.NONE) {
            return List.of();
        }
        GenType type = roll.type;
        int x = roll.x;
        int z = roll.z;

        List<OilGenStructure> structures = new ArrayList<>();
        int lakeRadius;
        int tendrilRadius;
        if (type == GenType.LARGE) {
            lakeRadius = 4;
            tendrilRadius = 25 + rand.nextInt(20);
        } else if (type == GenType.LAKE) {
            lakeRadius = 6;
            tendrilRadius = 25 + rand.nextInt(20);
        } else {
            lakeRadius = 2;
            tendrilRadius = 5 + rand.nextInt(10);
        }
        structures.add(createTendril(new BlockPos(x, 62, z), lakeRadius, tendrilRadius, rand, minY, maxY));

        if (type != GenType.LAKE) {
            // Generate a spherical cave deposit
            int wellY = 20 + rand.nextInt(10);

            int radius;
            if (type == GenType.LARGE) {
                radius = 8 + rand.nextInt(9);
            } else {
                radius = 4 + rand.nextInt(4);
            }

            structures.add(createSphere(new BlockPos(x, wellY, z), radius));

            // Generate a spout
            if (BCEnergyConfig.enableOilSpouts()) {
                int maxHeight, minHeight;

                if (type == GenType.LARGE) {
                    minHeight = BCEnergyConfig.largeSpoutMinHeight();
                    maxHeight = BCEnergyConfig.largeSpoutMaxHeight();
                    radius = 1;
                } else {
                    minHeight = BCEnergyConfig.smallSpoutMinHeight();
                    maxHeight = BCEnergyConfig.smallSpoutMaxHeight();
                    radius = 0;
                }
                final int height;
                if (maxHeight == minHeight) {
                    height = maxHeight;
                } else {
                    if (maxHeight < minHeight) {
                        int t = maxHeight;
                        maxHeight = minHeight;
                        minHeight = t;
                    }
                    height = minHeight + rand.nextInt(maxHeight - minHeight);
                }
                structures.add(new OilGenStructure.Spout(new BlockPos(x, wellY, z), radius, height, maxY));
            }
            // 1.12 also put an oil spring (which slowly refills the deposit when pumped) at the bottom of large
            // deposits: it will come with the pump.
        }
        return structures;
    }

    public static OilGenStructure createTube(BlockPos center, int length, int radius, Direction.Axis axis) {
        int valForAxis = axis.choose(center.getX(), center.getY(), center.getZ());
        BlockPos min = withValue(center.offset(-radius, -radius, -radius), axis, valForAxis);
        BlockPos max = withValue(center.offset(radius, radius, radius), axis, valForAxis + length);
        double radiusSq = radius * radius;
        Predicate<BlockPos> tester = p -> withValue(p, axis, valForAxis).distSqr(center) <= radiusSq;
        return new OilGenStructure.GenByPredicate(OilGenStructure.box(min, max), tester);
    }

    private static BlockPos withValue(BlockPos pos, Direction.Axis axis, int value) {
        return switch (axis) {
            case X -> new BlockPos(value, pos.getY(), pos.getZ());
            case Y -> new BlockPos(pos.getX(), value, pos.getZ());
            case Z -> new BlockPos(pos.getX(), pos.getY(), value);
        };
    }

    public static OilGenStructure createSphere(BlockPos center, int radius) {
        BoundingBox box = OilGenStructure.box(center.offset(-radius, -radius, -radius), center.offset(
            radius, radius, radius
        ));
        double radiusSq = radius * radius + 0.01;
        Predicate<BlockPos> tester = p -> p.distSqr(center) <= radiusSq;
        return new OilGenStructure.GenByPredicate(box, tester);
    }

    /** A lake in the middle of a pattern of thin oil tendrils, spreading out randomly. */
    public static OilGenStructure createTendril(BlockPos center, int lakeRadius, int radius, Random rand, int minY,
        int maxY) {
        BlockPos start = center.offset(-radius, 0, -radius);
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];

        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }

        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);

            fillPatternIfProba(rand, proba, x, z + w, pattern);
            fillPatternIfProba(rand, proba, x, z - w, pattern);
            fillPatternIfProba(rand, proba, x + w, z, pattern);
            fillPatternIfProba(rand, proba, x - w, z, pattern);

            for (int i = 1; i <= w; i++) {
                fillPatternIfProba(rand, proba, x + i, z + w, pattern);
                fillPatternIfProba(rand, proba, x + i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z + i, pattern);
                fillPatternIfProba(rand, proba, x - w, z + i, pattern);

                fillPatternIfProba(rand, proba, x - i, z + w, pattern);
                fillPatternIfProba(rand, proba, x - i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z - i, pattern);
                fillPatternIfProba(rand, proba, x - w, z - i, pattern);
            }
        }

        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return OilGenStructure.PatternTerrainHeight.create(start, pattern, depth, minY, maxY);
    }

    private static void fillPatternIfProba(Random rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) //
                | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        if (x < 0 || x >= pattern.length) return false;
        if (z < 0 || z >= pattern[x].length) return false;
        return pattern[x][z];
    }
}
