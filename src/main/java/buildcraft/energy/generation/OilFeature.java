/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import buildcraft.energy.BCEnergyConfig;

/** Generates the parts of every nearby oil deposit that reach into the chunk being decorated. Placed once per chunk. */
public class OilFeature extends Feature<NoneFeatureConfiguration> {
    public OilFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!BCEnergyConfig.enableOilGeneration()) {
            return false;
        }
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        BoundingBox box = new BoundingBox(
            chunk.getMinBlockX(), minY, chunk.getMinBlockZ(), chunk.getMaxBlockX(), maxY, chunk.getMaxBlockZ()
        );

        // Deposits can be centred in chunks that haven't generated yet, so their biome comes from the generator
        ServerLevel serverLevel = level.getLevel();
        RandomState randomState = serverLevel.getChunkSource().randomState();
        OilGenerator.BiomeLookup biomes = (x, z) -> {
            Holder<Biome> biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), randomState.sampler()
            );
            return biome.unwrapKey().map(ResourceKey::location).orElse((ResourceLocation) null);
        };

        boolean generated = false;
        for (int cdx = -OilGenerator.MAX_CHUNK_RADIUS; cdx <= OilGenerator.MAX_CHUNK_RADIUS; cdx++) {
            for (int cdz = -OilGenerator.MAX_CHUNK_RADIUS; cdz <= OilGenerator.MAX_CHUNK_RADIUS; cdz++) {
                for (OilGenStructure structure : OilGenerator.getStructures(
                    level.getSeed(), chunk.x + cdx, chunk.z + cdz, biomes, minY, maxY
                )) {
                    structure.generate(level, box);
                    generated = true;
                }
            }
        }
        return generated;
    }
}
