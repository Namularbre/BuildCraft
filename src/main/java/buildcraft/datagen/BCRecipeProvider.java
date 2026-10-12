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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.lib.BCTags;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.factory.BCFactoryBlocks;
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

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, BCEnergyBlocks.ENGINE_STONE.get())
            .pattern("www")
            .pattern(" g ")
            .pattern("GpG")
            .define('w', Tags.Items.COBBLESTONE)
            .define('g', Tags.Items.GLASS_COLORLESS)
            .define('G', BCTags.Items.GEARS_STONE)
            .define('p', Items.PISTON)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_STONE))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, BCEnergyBlocks.ENGINE_IRON.get())
            .pattern("www")
            .pattern(" g ")
            .pattern("GpG")
            .define('w', Tags.Items.INGOTS_IRON)
            .define('g', Tags.Items.GLASS_COLORLESS)
            .define('G', BCTags.Items.GEARS_IRON)
            .define('p', Items.PISTON)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_IRON))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.TANK.get())
            .pattern("ggg")
            .pattern("g g")
            .pattern("ggg")
            .define('g', Tags.Items.GLASS_COLORLESS)
            .unlockedBy("has_glass", has(Tags.Items.GLASS_COLORLESS))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.CHUTE.get())
            .pattern("ici")
            .pattern("igi")
            .pattern(" i ")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('c', Tags.Items.CHESTS_WOODEN)
            .define('g', BCTags.Items.GEARS_STONE)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_STONE))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.HEAT_EXCHANGE.get())
            .pattern("igi")
            .pattern("###")
            .pattern("igi")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('g', BCTags.Items.GEARS_IRON)
            .define('#', Tags.Items.GLASS_COLORLESS)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_IRON))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.DISTILLER.get())
            .pattern("rtr")
            .pattern("tgt")
            .define('r', Items.REDSTONE_TORCH)
            .define('t', BCFactoryBlocks.TANK.get())
            .define('g', BCTags.Items.GEARS_DIAMOND)
            .unlockedBy("has_tank", has(BCFactoryBlocks.TANK.get()))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.MINING_WELL.get())
            .pattern("iri")
            .pattern("igi")
            .pattern("ipi")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', BCTags.Items.GEARS_IRON)
            .define('p', Items.IRON_PICKAXE)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_IRON))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.PUMP.get())
            .pattern("iri")
            .pattern("igi")
            .pattern("tbt")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', BCTags.Items.GEARS_IRON)
            .define('t', BCFactoryBlocks.TANK.get())
            .define('b', Items.BUCKET)
            .unlockedBy("has_tank", has(BCFactoryBlocks.TANK.get()))
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

        Ingredient redstone = Ingredient.of(Tags.Items.DUSTS_REDSTONE);
        pipeUpgrade(out, BCTransportItems.PIPE_WOOD_ITEM.get(), BCTransportItems.PIPE_WOOD_POWER.get(), redstone);
        pipeUpgrade(
            out, BCTransportItems.PIPE_COBBLESTONE_ITEM.get(), BCTransportItems.PIPE_COBBLESTONE_POWER.get(), redstone
        );
        pipeUpgrade(out, BCTransportItems.PIPE_STONE_ITEM.get(), BCTransportItems.PIPE_STONE_POWER.get(), redstone);
        pipeUpgrade(out, BCTransportItems.PIPE_QUARTZ_ITEM.get(), BCTransportItems.PIPE_QUARTZ_POWER.get(), redstone);
        pipeUpgrade(out, BCTransportItems.PIPE_GOLD_ITEM.get(), BCTransportItems.PIPE_GOLD_POWER.get(), redstone);
        pipeUpgrade(
            out, BCTransportItems.PIPE_SANDSTONE_ITEM.get(), BCTransportItems.PIPE_SANDSTONE_POWER.get(), redstone
        );

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, BCTransportItems.PIPE_SEALANT.get())
            .requires(Ingredient.of(Items.GREEN_DYE, Items.SLIME_BALL))
            .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
            .unlockedBy("has_green_dye", has(Items.GREEN_DYE))
            .save(out);
        Ingredient sealant = Ingredient.of(BCTransportItems.PIPE_SEALANT.get());
        pipeUpgrade(out, BCTransportItems.PIPE_WOOD_ITEM.get(), BCTransportItems.PIPE_WOOD_FLUID.get(), sealant);
        pipeUpgrade(
            out, BCTransportItems.PIPE_COBBLESTONE_ITEM.get(), BCTransportItems.PIPE_COBBLESTONE_FLUID.get(), sealant
        );
        pipeUpgrade(out, BCTransportItems.PIPE_STONE_ITEM.get(), BCTransportItems.PIPE_STONE_FLUID.get(), sealant);
        pipeUpgrade(out, BCTransportItems.PIPE_QUARTZ_ITEM.get(), BCTransportItems.PIPE_QUARTZ_FLUID.get(), sealant);
        pipeUpgrade(out, BCTransportItems.PIPE_GOLD_ITEM.get(), BCTransportItems.PIPE_GOLD_FLUID.get(), sealant);
        pipeUpgrade(
            out, BCTransportItems.PIPE_SANDSTONE_ITEM.get(), BCTransportItems.PIPE_SANDSTONE_FLUID.get(), sealant
        );
        pipeUpgrade(out, BCTransportItems.PIPE_IRON_ITEM.get(), BCTransportItems.PIPE_IRON_FLUID.get(), sealant);
        pipeUpgrade(out, BCTransportItems.PIPE_VOID_ITEM.get(), BCTransportItems.PIPE_VOID_FLUID.get(), sealant);
    }

    /** Turns a pipe into another by adding an item, and back again (losing the item). */
    private static void pipeUpgrade(Consumer<FinishedRecipe> out, ItemLike from, ItemLike to, Ingredient additional) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, to)
            .requires(from)
            .requires(additional)
            .unlockedBy("has_pipe", has(from))
            .save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, from)
            .requires(to)
            .unlockedBy("has_pipe", has(to))
            .save(out, ForgeRegistries.ITEMS.getKey(to.asItem()) + "_undo");
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
