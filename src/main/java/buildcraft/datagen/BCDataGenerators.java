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
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import buildcraft.BuildCraft;

/** Generates the JSON resources (block states, models, recipes, tags, loot tables, translations). Run with
 * {@code ./gradlew runData}, and commit the output in src/generated. */
@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BCDataGenerators {
    private BCDataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new BCBlockStateProvider(output, files));
        generator.addProvider(event.includeClient(), new BCItemModelProvider(output, files));
        generator.addProvider(event.includeClient(), new BCLanguageProvider.English(output));
        generator.addProvider(event.includeClient(), new BCLanguageProvider.French(output));

        BCTagProviders.Blocks blockTags = new BCTagProviders.Blocks(output, lookup, files);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(
            event.includeServer(), new BCTagProviders.Items(output, lookup, blockTags.contentsGetter(), files)
        );
        generator.addProvider(event.includeServer(), new BCRecipeProvider(output));
        generator.addProvider(
            event.includeServer(), new LootTableProvider(
                output, Set.of(), List.of(
                    new LootTableProvider.SubProviderEntry(BCBlockLootProvider::new, LootContextParamSets.BLOCK)
                )
            )
        );
    }
}
