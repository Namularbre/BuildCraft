/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.Tags;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipeBasic;
import buildcraft.api.recipes.IngredientStack;

import buildcraft.lib.recipe.AssemblyRecipeRegistry;

import buildcraft.BuildCraft;

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
    }

    private static void chipset(EnumRedstoneChipset chipset, long mj, Set<IngredientStack> input) {
        ResourceLocation name = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, chipset.getItemName());
        ItemStack output = new ItemStack(BCSiliconItems.chipset(chipset));
        AssemblyRecipeRegistry.register(new AssemblyRecipeBasic(name, mj * MjAPI.MJ, input, output));
    }
}
