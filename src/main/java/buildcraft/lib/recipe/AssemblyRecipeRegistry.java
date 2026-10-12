/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.recipe;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.recipes.AssemblyRecipe;

/** Every recipe of the assembly table, by name. */
public final class AssemblyRecipeRegistry {
    public static final Map<ResourceLocation, AssemblyRecipe> REGISTRY = new TreeMap<>();

    private AssemblyRecipeRegistry() {}

    public static void register(AssemblyRecipe recipe) {
        REGISTRY.put(recipe.getRegistryName(), recipe);
    }
}
