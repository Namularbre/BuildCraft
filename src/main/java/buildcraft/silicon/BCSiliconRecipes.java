/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.common.Tags;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipeBasic;
import buildcraft.api.recipes.IngredientStack;

import buildcraft.lib.recipe.AssemblyRecipeRegistry;

import buildcraft.BuildCraft;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;

/** The assembly table recipes, with the values of 1.12. */
public final class BCSiliconRecipes {
    private BCSiliconRecipes() {}

    /** Must be called once the items are registered. */
    public static void register() {
        IngredientStack redstone = IngredientStack.of(Tags.Items.DUSTS_REDSTONE);
        chipset(EnumRedstoneChipset.RED, 10_000, Set.of(redstone));
        chipset(EnumRedstoneChipset.IRON, 20_000, Set.of(redstone, IngredientStack.of(Tags.Items.INGOTS_IRON)));
        chipset(EnumRedstoneChipset.GOLD, 40_000, Set.of(redstone, IngredientStack.of(Tags.Items.INGOTS_GOLD)));
        chipset(EnumRedstoneChipset.QUARTZ, 60_000, Set.of(redstone, IngredientStack.of(Tags.Items.GEMS_QUARTZ)));
        chipset(EnumRedstoneChipset.DIAMOND, 80_000, Set.of(redstone, IngredientStack.of(Tags.Items.GEMS_DIAMOND)));

        gate(20_000, EnumGateMaterial.IRON, EnumRedstoneChipset.IRON);
        gate(40_000, EnumGateMaterial.NETHER_BRICK, EnumRedstoneChipset.IRON, IngredientStack.of(Items.NETHER_BRICKS));
        gate(80_000, EnumGateMaterial.GOLD, EnumRedstoneChipset.GOLD);

        IngredientStack lapis = IngredientStack.of(Tags.Items.GEMS_LAPIS);
        IngredientStack quartz = IngredientStack.of(BCSiliconItems.chipset(EnumRedstoneChipset.QUARTZ));
        IngredientStack diamond = IngredientStack.of(BCSiliconItems.chipset(EnumRedstoneChipset.DIAMOND));
        gateModifier(40_000, EnumGateMaterial.IRON, EnumGateModifier.LAPIS, lapis);
        gateModifier(60_000, EnumGateMaterial.IRON, EnumGateModifier.QUARTZ, quartz);
        gateModifier(80_000, EnumGateMaterial.IRON, EnumGateModifier.DIAMOND, diamond);
        gateModifier(80_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.LAPIS, lapis);
        gateModifier(100_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.QUARTZ, quartz);
        gateModifier(120_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.DIAMOND, diamond);
        gateModifier(100_000, EnumGateMaterial.GOLD, EnumGateModifier.LAPIS, lapis);
        gateModifier(140_000, EnumGateMaterial.GOLD, EnumGateModifier.QUARTZ, quartz);
        gateModifier(180_000, EnumGateMaterial.GOLD, EnumGateModifier.DIAMOND, diamond);
    }

    /** Both the AND and OR gates (without modifier) of the given material, from a chipset (and more). */
    private static void gate(long mj, EnumGateMaterial material, EnumRedstoneChipset chipset,
        IngredientStack... additional) {
        Set<IngredientStack> input = new HashSet<>(Arrays.asList(additional));
        input.add(IngredientStack.of(BCSiliconItems.chipset(chipset)));
        for (EnumGateLogic logic : EnumGateLogic.VALUES) {
            register(mj, new GateVariant(logic, material, EnumGateModifier.NO_MODIFIER), input);
        }
    }

    /** Adds a modifier to the AND and OR gates of the given material. */
    private static void gateModifier(long mj, EnumGateMaterial material, EnumGateModifier modifier,
        IngredientStack mod) {
        for (EnumGateLogic logic : EnumGateLogic.VALUES) {
            Item from = BCSiliconItems.gate(new GateVariant(logic, material, EnumGateModifier.NO_MODIFIER));
            register(mj, new GateVariant(logic, material, modifier), Set.of(IngredientStack.of(from), mod));
        }
    }

    private static void register(long mj, GateVariant variant, Set<IngredientStack> input) {
        ResourceLocation name = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, variant.getItemName());
        ItemStack output = new ItemStack(BCSiliconItems.gate(variant));
        AssemblyRecipeRegistry.register(new AssemblyRecipeBasic(name, mj * MjAPI.MJ, Set.copyOf(input), output));
    }

    private static void chipset(EnumRedstoneChipset chipset, long mj, Set<IngredientStack> input) {
        ResourceLocation name = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, chipset.getItemName());
        ItemStack output = new ItemStack(BCSiliconItems.chipset(chipset));
        AssemblyRecipeRegistry.register(new AssemblyRecipeBasic(name, mj * MjAPI.MJ, input, output));
    }
}
