/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.silicon.recipe.FacadeSwapRecipe;

public final class BCSiliconRecipeSerializers {
    public static final RegistryObject<SimpleCraftingRecipeSerializer<FacadeSwapRecipe>> FACADE_SWAP =
        BCRegistries.RECIPE_SERIALIZERS.register("facade_swap",
            () -> new SimpleCraftingRecipeSerializer<>(FacadeSwapRecipe::new));

    private BCSiliconRecipeSerializers() {}

    /** Loads this class, so that every entry is added to the registers. */
    static void init() {}
}
