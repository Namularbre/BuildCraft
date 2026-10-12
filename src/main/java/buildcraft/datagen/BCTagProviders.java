/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import buildcraft.lib.BCTags;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.factory.BCFactoryBlocks;

public final class BCTagProviders {
    private BCTagProviders() {}

    public static final class Blocks extends BlockTagsProvider {
        public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(output, lookup, BuildCraft.MODID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BlockTags.MINEABLE_WITH_AXE).add(BCCoreBlocks.ENGINE_REDSTONE.get());
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BCEnergyBlocks.ENGINE_STONE.get())
                .add(BCEnergyBlocks.ENGINE_IRON.get()).add(BCFactoryBlocks.PUMP.get())
                .add(BCFactoryBlocks.MINING_WELL.get()).add(BCFactoryBlocks.DISTILLER.get())
                .add(BCFactoryBlocks.HEAT_EXCHANGE.get()).add(BCFactoryBlocks.CHUTE.get());
            if (BCCoreBlocks.POWER_TESTER != null) {
                // Optional: the tester only exists in development, and a missing entry would break the whole tag
                tag(BlockTags.MINEABLE_WITH_PICKAXE).addOptional(BCCoreBlocks.POWER_TESTER.getId());
            }
        }
    }

    public static final class Items extends ItemTagsProvider {
        public Items(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
            CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper files) {
            super(output, lookup, blockTags, BuildCraft.MODID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(BCTags.Items.GEARS_WOOD).add(BCCoreItems.GEAR_WOOD.get());
            tag(BCTags.Items.GEARS_STONE).add(BCCoreItems.GEAR_STONE.get());
            tag(BCTags.Items.GEARS_IRON).add(BCCoreItems.GEAR_IRON.get());
            tag(BCTags.Items.GEARS_GOLD).add(BCCoreItems.GEAR_GOLD.get());
            tag(BCTags.Items.GEARS_DIAMOND).add(BCCoreItems.GEAR_DIAMOND.get());
            tag(BCTags.Items.GEARS).addTags(
                BCTags.Items.GEARS_WOOD, BCTags.Items.GEARS_STONE, BCTags.Items.GEARS_IRON, BCTags.Items.GEARS_GOLD,
                BCTags.Items.GEARS_DIAMOND
            );
            tag(BCTags.Items.WRENCHES).add(BCCoreItems.WRENCH.get());
            tag(BCTags.Items.TOOLS).addTag(BCTags.Items.WRENCHES);
        }
    }
}
