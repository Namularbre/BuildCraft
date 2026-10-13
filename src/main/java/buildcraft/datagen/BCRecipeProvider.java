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
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.lib.BCTags;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryItems;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipeSerializers;
import buildcraft.silicon.EnumRedstoneChipset;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCCoreBlocks.MARKER_VOLUME.get())
            .pattern("l")
            .pattern("t")
            .define('l', Tags.Items.GEMS_LAPIS)
            .define('t', Items.REDSTONE_TORCH)
            .unlockedBy("has_lapis", has(Tags.Items.GEMS_LAPIS))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.TEMPLATE.get())
            .pattern("ppp")
            .pattern("pbp")
            .pattern("ppp")
            .define('p', Items.PAPER)
            .define('b', Tags.Items.DYES_BLACK)
            .unlockedBy("has_paper", has(Items.PAPER))
            .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.BLUEPRINT.get())
            .pattern("ppp")
            .pattern("plp")
            .pattern("ppp")
            .define('p', Items.PAPER)
            .define('l', Tags.Items.GEMS_LAPIS)
            .unlockedBy("has_paper", has(Items.PAPER))
            .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.ARCHITECT.get())
            .pattern("bmb")
            .pattern("ycy")
            .pattern("dsd")
            .define('b', Tags.Items.DYES_BLACK)
            .define('m', BCCoreBlocks.MARKER_VOLUME.get())
            .define('y', Tags.Items.DYES_YELLOW)
            .define('c', Items.CRAFTING_TABLE)
            .define('d', BCTags.Items.GEARS_DIAMOND)
            .define('s', BCBuildersBlocks.BLUEPRINT.get())
            .unlockedBy("has_marker", has(BCCoreBlocks.MARKER_VOLUME.get()))
            .save(out);
        // Not craftable in 1.12 (it was creative only): like the filler, but with diamond gears
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.BUILDER.get())
            .pattern("bmb")
            .pattern("ycy")
            .pattern("dCd")
            .define('b', Tags.Items.DYES_BLACK)
            .define('m', BCCoreBlocks.MARKER_VOLUME.get())
            .define('y', Tags.Items.DYES_YELLOW)
            .define('c', Items.CRAFTING_TABLE)
            .define('d', BCTags.Items.GEARS_DIAMOND)
            .define('C', Tags.Items.CHESTS_WOODEN)
            .unlockedBy("has_marker", has(BCCoreBlocks.MARKER_VOLUME.get()))
            .save(out);
        // Not craftable in 1.12 (it was creative only): this is the recipe of BuildCraft 7
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.FILLER.get())
            .pattern("bmb")
            .pattern("ycy")
            .pattern("gCg")
            .define('b', Tags.Items.DYES_BLACK)
            .define('m', BCCoreBlocks.MARKER_VOLUME.get())
            .define('y', Tags.Items.DYES_YELLOW)
            .define('c', Items.CRAFTING_TABLE)
            .define('g', BCTags.Items.GEARS_GOLD)
            .define('C', Tags.Items.CHESTS_WOODEN)
            .unlockedBy("has_marker", has(BCCoreBlocks.MARKER_VOLUME.get()))
            .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCBuildersBlocks.QUARRY.get())
            .pattern("iri")
            .pattern("gig")
            .pattern("dpd")
            .define('i', BCTags.Items.GEARS_IRON)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', BCTags.Items.GEARS_GOLD)
            .define('d', BCTags.Items.GEARS_DIAMOND)
            .define('p', Items.DIAMOND_PICKAXE)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_DIAMOND))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCSiliconBlocks.LASER.get())
            .pattern("rro")
            .pattern("rdd")
            .pattern("rro")
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('o', Items.OBSIDIAN)
            .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get())
            .pattern("OtO")
            .pattern("OcO")
            .pattern("OrO")
            .define('O', Items.OBSIDIAN)
            .define('t', Items.CRAFTING_TABLE)
            .define('c', Tags.Items.CHESTS_WOODEN)
            .define('r', BCSiliconItems.chipset(EnumRedstoneChipset.RED))
            .unlockedBy("has_chipset", has(BCSiliconItems.chipset(EnumRedstoneChipset.RED)))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCSiliconBlocks.ASSEMBLY_TABLE.get())
            .pattern("OdO")
            .pattern("OrO")
            .pattern("OgO")
            .define('O', Items.OBSIDIAN)
            .define('d', Tags.Items.GEMS_DIAMOND)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .define('g', BCTags.Items.GEARS_DIAMOND)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_DIAMOND))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryItems.WATER_GEL.get())
            .pattern(" s ")
            .pattern("srs")
            .pattern(" s ")
            .define('s', Items.SAND)
            .define('r', BCEnergyFluids.OIL_RESIDUE.get(0).bucket.get())
            .unlockedBy("has_residue", has(BCEnergyFluids.OIL_RESIDUE.get(0).bucket.get()))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.WATER_BUCKET)
            .pattern("g")
            .pattern("b")
            .define('g', BCFactoryItems.GEL.get())
            .define('b', Items.BUCKET)
            .unlockedBy("has_gel", has(BCFactoryItems.GEL.get()))
            .save(out, ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "water_gel_to_bucket"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.AUTO_WORKBENCH_ITEMS.get())
            .pattern("gwg")
            .define('g', BCTags.Items.GEARS_STONE)
            .define('w', Items.CRAFTING_TABLE)
            .unlockedBy("has_gear", has(BCTags.Items.GEARS_STONE))
            .save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BCFactoryBlocks.FLOOD_GATE.get())
            .pattern("igi")
            .pattern("btb")
            .pattern("ibi")
            .define('i', Tags.Items.INGOTS_IRON)
            .define('g', BCTags.Items.GEARS_IRON)
            .define('b', Items.IRON_BARS)
            .define('t', BCFactoryBlocks.TANK.get())
            .unlockedBy("has_tank", has(BCFactoryBlocks.TANK.get()))
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
        pipe(out, BCTransportItems.PIPE_DIAMOND_ITEM.get(), Ingredient.of(Tags.Items.GEMS_DIAMOND));
        pipe(out, BCTransportItems.PIPE_LAPIS_ITEM.get(), Ingredient.of(Tags.Items.STORAGE_BLOCKS_LAPIS));
        pipe(out, BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get(), Ingredient.of(ItemTags.PLANKS),
            Ingredient.of(Tags.Items.GEMS_DIAMOND));
        pipeUpgrade(out, BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get(), BCTransportItems.PIPE_EMZULI_ITEM.get(),
            Ingredient.of(Tags.Items.STORAGE_BLOCKS_LAPIS));
        pipe(out, BCTransportItems.PIPE_DAIZULI_ITEM.get(), Ingredient.of(Tags.Items.STORAGE_BLOCKS_LAPIS),
            Ingredient.of(Tags.Items.GEMS_DIAMOND));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, BCTransportItems.PIPE_STRUCTURE.get(), 8)
            .pattern("cgc")
            .define('c', Tags.Items.COBBLESTONE)
            .define('g', Items.GRAVEL)
            .unlockedBy("has_gravel", has(Items.GRAVEL))
            .save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TRANSPORTATION, BCTransportItems.PLUG_BLOCKER.get(), 4)
            .requires(BCTransportItems.PIPE_STRUCTURE.get())
            .unlockedBy("has_structure_pipe", has(BCTransportItems.PIPE_STRUCTURE.get()))
            .save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, BCTransportItems.PLUG_POWER_ADAPTOR.get(), 4)
            .pattern("sis")
            .pattern("sgs")
            .pattern("srs")
            .define('s', BCTransportItems.PIPE_STRUCTURE.get())
            .define('i', Tags.Items.INGOTS_GOLD)
            .define('g', BCTags.Items.GEARS_STONE)
            .define('r', Tags.Items.DUSTS_REDSTONE)
            .unlockedBy("has_structure_pipe", has(BCTransportItems.PIPE_STRUCTURE.get()))
            .save(out);
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
        pipeUpgrade(
            out, BCTransportItems.PIPE_DIAMOND_ITEM.get(), BCTransportItems.PIPE_DIAMOND_FLUID.get(), sealant
        );
        pipeUpgrade(out, BCTransportItems.PIPE_VOID_ITEM.get(), BCTransportItems.PIPE_VOID_FLUID.get(), sealant);
        pipeUpgrade(out, BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get(), BCTransportItems.PIPE_DIAMOND_WOOD_FLUID.get(),
            sealant);

        // Some of the basic gates can be made in a crafting table (the others need an assembly table)
        Ingredient plug = Ingredient.of(BCTransportItems.PLUG_BLOCKER.get(), Items.COBBLESTONE);
        gate(out, EnumGateMaterial.CLAY_BRICK, EnumGateModifier.NO_MODIFIER, Ingredient.of(Items.BRICK), plug);
        gate(out, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER, Ingredient.of(Tags.Items.INGOTS_IRON), plug);
        gate(out, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.NO_MODIFIER, Ingredient.of(Items.NETHER_BRICK),
            plug);
        Ingredient ironGate = Ingredient.of(
            BCSiliconItems.gate(new GateVariant(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER))
        );
        gate(out, EnumGateMaterial.IRON, EnumGateModifier.LAPIS, Ingredient.of(Tags.Items.GEMS_LAPIS), ironGate);
        gate(out, EnumGateMaterial.IRON, EnumGateModifier.QUARTZ, Ingredient.of(Tags.Items.GEMS_QUARTZ), ironGate);

        // A facade turns into a hollow facade, and back
        SpecialRecipeBuilder.special(BCSiliconRecipeSerializers.FACADE_SWAP.get())
            .save(out, ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "facade_swap").toString());
        // Two facades and a pipe wire make a phased facade
        SpecialRecipeBuilder.special(BCSiliconRecipeSerializers.FACADE_PHASED.get())
            .save(out, ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "facade_phased").toString());

        // AND gates can be turned into OR gates, and back
        for (EnumGateMaterial material : EnumGateMaterial.VALUES) {
            if (!material.canBeModified) {
                continue;
            }
            for (EnumGateModifier modifier : EnumGateModifier.VALUES) {
                Item and = BCSiliconItems.gate(new GateVariant(EnumGateLogic.AND, material, modifier));
                Item or = BCSiliconItems.gate(new GateVariant(EnumGateLogic.OR, material, modifier));
                gateSwap(out, and, or);
                gateSwap(out, or, and);
            }
        }
    }

    /** A gate made of 4 of the given material around redstone, or with the given material around the center item. */
    private static void gate(Consumer<FinishedRecipe> out, EnumGateMaterial material, EnumGateModifier modifier,
        Ingredient around, Ingredient center) {
        Item result = BCSiliconItems.gate(new GateVariant(EnumGateLogic.AND, material, modifier));
        if (modifier == EnumGateModifier.NO_MODIFIER) {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, result)
                .pattern(" m ")
                .pattern("mrm")
                .pattern(" b ")
                .define('m', around)
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('b', center)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(out);
        } else {
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, result)
                .pattern(" m ")
                .pattern("mgm")
                .pattern(" m ")
                .define('m', around)
                .define('g', center)
                .unlockedBy("has_gate", has(center.getItems()[0].getItem()))
                .save(out);
        }
    }

    private static void gateSwap(Consumer<FinishedRecipe> out, Item from, Item to) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, to)
            .requires(from)
            .unlockedBy("has_gate", has(from))
            .save(out, ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID,
                ForgeRegistries.ITEMS.getKey(to).getPath() + "_from_" + ForgeRegistries.ITEMS.getKey(from).getPath()));
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
