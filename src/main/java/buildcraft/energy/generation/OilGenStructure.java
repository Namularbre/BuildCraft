/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import buildcraft.energy.BCEnergyFluids;

/** A part of an oil deposit. Deposits span several chunks, so every part can generate just the blocks that are within
 * a given box (the chunk being generated). */
public abstract class OilGenStructure {
    public final BoundingBox box;

    public OilGenStructure(BoundingBox box) {
        this.box = box;
    }

    public final void generate(WorldGenLevel level, BoundingBox within) {
        BoundingBox intersect = intersect(box, within);
        if (intersect != null) {
            generateWithin(level, intersect);
        }
    }

    /** Generates this structure in the world, but only between the given coordinates. */
    protected abstract void generateWithin(WorldGenLevel level, BoundingBox intersect);

    @Nullable
    static BoundingBox intersect(BoundingBox a, BoundingBox b) {
        if (!a.intersects(b)) {
            return null;
        }
        return new BoundingBox(
            Math.max(a.minX(), b.minX()), Math.max(a.minY(), b.minY()), Math.max(a.minZ(), b.minZ()), Math.min(
                a.maxX(), b.maxX()
            ), Math.min(a.maxY(), b.maxY()), Math.min(a.maxZ(), b.maxZ())
        );
    }

    static BoundingBox box(BlockPos min, BlockPos max) {
        return BoundingBox.fromCorners(min, max);
    }

    public static void setOil(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, BCEnergyFluids.CRUDE_OIL.get(0).block.get().defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Oil wherever the predicate matches. */
    public static class GenByPredicate extends OilGenStructure {
        public final Predicate<BlockPos> predicate;

        public GenByPredicate(BoundingBox box, Predicate<BlockPos> predicate) {
            super(box);
            this.predicate = predicate;
        }

        @Override
        protected void generateWithin(WorldGenLevel level, BoundingBox intersect) {
            for (BlockPos pos : BlockPos.betweenClosed(
                intersect.minX(), intersect.minY(), intersect.minZ(), intersect.maxX(), intersect.maxY(), intersect
                    .maxZ()
            )) {
                if (predicate.test(pos)) {
                    setOil(level, pos);
                }
            }
        }
    }

    /** A flat pattern of oil at the surface of the terrain (lakes and the tendrils around them), clearing the few
     * blocks above it. */
    public static class PatternTerrainHeight extends OilGenStructure {
        private final boolean[][] pattern;
        private final int depth;

        private PatternTerrainHeight(BoundingBox box, boolean[][] pattern, int depth) {
            super(box);
            this.pattern = pattern;
            this.depth = depth;
        }

        public static PatternTerrainHeight create(BlockPos start, boolean[][] pattern, int depth, int minY,
            int maxY) {
            BlockPos min = new BlockPos(start.getX(), minY, start.getZ());
            BlockPos max = new BlockPos(
                start.getX() + pattern.length - 1, maxY, start.getZ() + (pattern.length == 0 ? 0 : pattern[0].length
                    - 1)
            );
            return new PatternTerrainHeight(box(min, max), pattern, depth);
        }

        @Override
        protected void generateWithin(WorldGenLevel level, BoundingBox intersect) {
            for (int x = intersect.minX(); x <= intersect.maxX(); x++) {
                int px = x - box.minX();

                for (int z = intersect.minZ(); z <= intersect.maxZ(); z++) {
                    int pz = z - box.minZ();

                    if (pattern[px][pz]) {
                        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                        BlockPos upper = new BlockPos(x, top, z);
                        for (int y = 0; y < 5; y++) {
                            level.setBlock(upper.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                        for (int y = 0; y < depth; y++) {
                            setOil(level, upper.below(y));
                        }
                    }
                }
            }
        }
    }

    /** A column of oil from an underground deposit up to the surface, which then spurts up into the air. */
    public static class Spout extends OilGenStructure {
        public final BlockPos start;
        public final int radius;
        public final int height;

        public Spout(BlockPos start, int radius, int height, int maxY) {
            // Only a single column, so that it is generated once (by the chunk containing its start)
            super(box(start, new BlockPos(start.getX(), maxY, start.getZ())));
            this.start = start;
            this.radius = radius;
            this.height = height;
        }

        @Override
        protected void generateWithin(WorldGenLevel level, BoundingBox intersect) {
            // The surface, including water (so spouts in oceans start from the water's surface)
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, start.getX(), start.getZ());
            BlockPos worldTop = new BlockPos(start.getX(), surface, start.getZ());

            OilGenStructure tubeY = OilGenerator.createTube(
                start, worldTop.getY() - start.getY(), radius, Direction.Axis.Y
            );
            tubeY.generate(level, tubeY.box);
            BlockPos base = worldTop;
            for (int r = radius; r >= 0; r--) {
                OilGenStructure struct = OilGenerator.createTube(base, height, r, Direction.Axis.Y);
                struct.generate(level, struct.box);
                base = base.above(height);
            }
        }
    }
}
