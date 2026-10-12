/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import java.util.Set;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryItems;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockWaterGel.GelStage;

/** Every BuildCraft block that has an item drops itself (except gel, which has its own drops). Blocks without one
 * (like pipes) decide their drops in code. */
public class BCBlockLootProvider extends BlockLootSubProvider {
    public BCBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            if (block != BCFactoryBlocks.WATER_GEL.get()) {
                dropSelf(block);
            }
        }
        waterGel();
    }

    /** Gel drops 1 gel, or 1 to 2 while it is still spreading. */
    private void waterGel() {
        BlockWaterGel block = BCFactoryBlocks.WATER_GEL.get();
        AnyOfCondition.Builder spreading = AnyOfCondition.anyOf();
        for (GelStage stage : GelStage.values()) {
            if (stage.spreading) {
                StatePropertiesPredicate.Builder isStage =
                    StatePropertiesPredicate.Builder.properties().hasProperty(BlockWaterGel.STAGE, stage);
                spreading.or(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).setProperties(isStage));
            }
        }
        add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .add(LootItem.lootTableItem(BCFactoryItems.GEL.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)).when(spreading))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BCRegistries.BLOCKS.getEntries().stream().map(RegistryObject::get).map(Block.class::cast)
            .filter(block -> block.asItem() != Items.AIR || block == BCFactoryBlocks.WATER_GEL.get())::iterator;
    }
}
