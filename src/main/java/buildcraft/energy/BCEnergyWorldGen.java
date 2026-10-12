/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.BuildCraft;
import buildcraft.energy.generation.OilFeature;

/** Oil generation. The feature is added to every overworld biome by a biome modifier (see the data generators). */
public final class BCEnergyWorldGen {
    public static final RegistryObject<OilFeature> OIL_FEATURE = BCRegistries.FEATURES.register("oil", OilFeature::new);

    public static final ResourceKey<ConfiguredFeature<?, ?>> OIL_CONFIGURED = ResourceKey.create(
        Registries.CONFIGURED_FEATURE, id("oil")
    );
    public static final ResourceKey<PlacedFeature> OIL_PLACED = ResourceKey.create(
        Registries.PLACED_FEATURE, id("oil")
    );
    public static final ResourceKey<BiomeModifier> OIL_BIOME_MODIFIER = ResourceKey.create(
        ForgeRegistries.Keys.BIOME_MODIFIERS, id("oil")
    );

    private BCEnergyWorldGen() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, path);
    }

    /** Loads this class, so that the feature is added to the registers. */
    static void init() {}
}
