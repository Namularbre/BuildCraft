/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.BCEnergyWorldGen;
import buildcraft.energy.generation.OilGenerator;
import buildcraft.energy.generation.OilGenerator.GenType;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class OilWorldGenGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    /** Far from the test structures, so that generating oil there doesn't disturb them. */
    private static final int SEARCH_START_CHUNK = 500;

    /** Oil deposits only depend on the seed and the chunk, so the same chunk always holds the same deposit. */
    @GameTest(template = EMPTY)
    public static void oilDepositsAreDeterministic(GameTestHelper helper) {
        OilGenerator.BiomeLookup plains = (x, z) -> ResourceLocation.parse("minecraft:plains");
        int deposits = 0;
        for (int cx = 0; cx < 100; cx++) {
            for (int cz = 0; cz < 100; cz++) {
                GenType type = OilGenerator.getType(1234L, cx, cz, plains);
                helper.assertTrue(type == OilGenerator.getType(1234L, cx, cz, plains), "Same chunk, same deposit");
                helper.assertTrue(type != GenType.LAKE, "Lakes only generate in surface deposit biomes");
                if (type != GenType.NONE) {
                    deposits++;
                }
            }
        }
        // 0.04% large + ~0.1% medium of 10,000 chunks: around 14
        helper.assertTrue(deposits > 2 && deposits < 50, "Expected around 14 deposits, found " + deposits);
        helper.succeed();
    }

    /** The oil feature generates a deposit underground, even across chunk borders. */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void oilFeatureGeneratesDeposits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OilGenerator.BiomeLookup biomes = (x, z) -> {
            Holder<Biome> biome = level.getBiome(new BlockPos(x, 64, z));
            return biome.unwrapKey().map(ResourceKey::location).orElse(null);
        };
        // Find a chunk with a deposit in the test world
        ChunkPos deposit = null;
        for (int cx = SEARCH_START_CHUNK; cx < SEARCH_START_CHUNK + 200 && deposit == null; cx++) {
            for (int cz = 0; cz < 200; cz++) {
                GenType type = OilGenerator.getType(level.getSeed(), cx, cz, biomes);
                if (type == GenType.LARGE || type == GenType.MEDIUM) {
                    deposit = new ChunkPos(cx, cz);
                    break;
                }
            }
        }
        helper.assertTrue(deposit != null, "There should be a deposit in 40,000 chunks");

        // The deposit is centred somewhere in this chunk or the next one: generate all of them
        for (int cx = deposit.x - 1; cx <= deposit.x + 2; cx++) {
            for (int cz = deposit.z - 1; cz <= deposit.z + 2; cz++) {
                BlockPos origin = new ChunkPos(cx, cz).getWorldPosition();
                BCEnergyWorldGen.OIL_FEATURE.get().place(new FeaturePlaceContext<>(
                    Optional.empty(), level, level.getChunkSource().getGenerator(), level.getRandom(), origin,
                    NoneFeatureConfiguration.INSTANCE
                ));
            }
        }

        int oil = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
            deposit.getMinBlockX() - 16, 10, deposit.getMinBlockZ() - 16, deposit.getMaxBlockX() + 32, 40, deposit
                .getMaxBlockZ() + 32
        )) {
            if (level.getFluidState(pos).getType().isSame(BCEnergyFluids.CRUDE_OIL.get(0).get())) {
                oil++;
            }
        }
        // The smallest deposit is a sphere of radius 4
        helper.assertTrue(oil > 100, "The deposit at " + deposit + " should have generated oil, found " + oil);
        helper.succeed();
    }
}
