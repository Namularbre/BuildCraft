/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.BuildCraft;
import buildcraft.energy.BCEnergyWorldGen;

/** World generation: oil deposits in every overworld biome. */
public class BCWorldGenProvider extends DatapackBuiltinEntriesProvider {
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
        .add(Registries.CONFIGURED_FEATURE, context -> context.register(
            BCEnergyWorldGen.OIL_CONFIGURED, new ConfiguredFeature<>(
                BCEnergyWorldGen.OIL_FEATURE.get(), NoneFeatureConfiguration.INSTANCE
            )
        ))
        // No placement modifiers: placed once per chunk, at its corner
        .add(Registries.PLACED_FEATURE, context -> context.register(
            BCEnergyWorldGen.OIL_PLACED, new PlacedFeature(
                context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(BCEnergyWorldGen.OIL_CONFIGURED), List.of()
            )
        ))
        // At the same point as 1.12 (before lakes)
        .add(ForgeRegistries.Keys.BIOME_MODIFIERS, context -> context.register(
            BCEnergyWorldGen.OIL_BIOME_MODIFIER, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                context.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD), HolderSet.direct(
                    context.lookup(Registries.PLACED_FEATURE).getOrThrow(BCEnergyWorldGen.OIL_PLACED)
                ), GenerationStep.Decoration.LAKES
            )
        ));

    public BCWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, BUILDER, Set.of(BuildCraft.MODID));
    }
}
