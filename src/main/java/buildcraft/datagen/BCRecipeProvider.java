/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import java.util.function.Consumer;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.common.Tags;

import buildcraft.lib.BCTags;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.transport.BCTransportItems;

/** Crafting recipes, ported from the 1.12 recipe JSONs (ore dictionary entries became tags). */
public class BCRecipeProvider extends RecipeProvider {
    public BCRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreItems.GEAR_WOOD.get())
            .pattern(" o ")
            .pattern("o o")
            .pattern(" o ")
            .define('o', Tags.Items.RODS_WOODEN)
            .group("buildcraft:gears")
            .unlockedBy("has_stick", has(Tags.Items.RODS_WOODEN))
            .save(out);
        gear(out, BCCoreItems.GEAR_STONE.get(), Tags.Items.COBBLESTONE, BCTags.Items.GEARS_WOOD);
        gear(out, BCCoreItems.GEAR_IRON.get(), Tags.Items.INGOTS_IRON, BCTags.Items.GEARS_STONE);
        gear(out, BCCoreItems.GEAR_GOLD.get(), Tags.Items.INGOTS_GOLD, BCTags.Items.GEARS_IRON);
        gear(out, BCCoreItems.GEAR_DIAMOND.get(), Tags.Items.GEMS_DIAMOND, BCTags.Items.GEARS_GOLD);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BCCoreItems.WRENCH.get())
            .pattern("I I")
            .pattern(" G ")
            .pattern(" I ")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('G', BCTags.Items.GEARS_STONE)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_STONE))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, BCCoreBlocks.ENGINE_REDSTONE.get())
            .pattern("www")
            .pattern(" g ")
            .pattern("GpG")
            .define('w', ItemTags.PLANKS)
            .define('g', Tags.Items.GLASS_COLORLESS)
            .define('G', BCTags.Items.GEARS_WOOD)
            .define('p', Items.PISTON)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_WOOD))
            .save(out);

        pipe(out, BCTransportItems.PIPE_WOOD_ITEM.get(), Ingredient.of(ItemTags.PLANKS));
        pipe(out, BCTransportItems.PIPE_COBBLESTONE_ITEM.get(), Ingredient.of(Tags.Items.COBBLESTONE));
        pipe(out, BCTransportItems.PIPE_STONE_ITEM.get(), Ingredient.of(Items.STONE));
        pipe(out, BCTransportItems.PIPE_QUARTZ_ITEM.get(), Ingredient.of(Items.QUARTZ_BLOCK));
        pipe(out, BCTransportItems.PIPE_GOLD_ITEM.get(), Ingredient.of(Tags.Items.INGOTS_GOLD));
        pipe(out, BCTransportItems.PIPE_SANDSTONE_ITEM.get(), Ingredient.of(Tags.Items.SANDSTONE));
        pipe(out, BCTransportItems.PIPE_IRON_ITEM.get(), Ingredient.of(Tags.Items.INGOTS_IRON));
        pipe(
            out, BCTransportItems.PIPE_VOID_ITEM.get(), Ingredient.of(Tags.Items.DYES_BLACK),
            Ingredient.of(Tags.Items.DUSTS_REDSTONE)
        );
    }

    /** 8 pipes from a piece of colourless glass between two of the material. */
    private static void pipe(Consumer<FinishedRecipe> out, ItemLike result, Ingredient material) {
        pipe(out, result, material, material);
    }

    private static void pipe(Consumer<FinishedRecipe> out, ItemLike result, Ingredient left, Ingredient right) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, result, 8)
            .pattern("lgr")
            .define('l', left)
            .define('g', Tags.Items.GLASS_COLORLESS)
            .define('r', right)
            .group("buildcraft:pipes")
            .unlockedBy("has_glass", has(Tags.Items.GLASS_COLORLESS))
            .save(out);
    }

    /** A gear made of 4 of the material around the previous tier's gear. */
    private static void gear(Consumer<FinishedRecipe> out, ItemLike result, TagKey<Item> material,
        TagKey<Item> previousGear) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
            .pattern(" o ")
            .pattern("oio")
            .pattern(" o ")
            .define('o', material)
            .define('i', previousGear)
            .group("buildcraft:gears")
            .unlockedBy("has_gear", has(previousGear))
            .save(out);
    }
}
