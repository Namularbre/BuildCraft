/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipeSerializers;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeInstance;

/** A facade alone in a crafting grid turns into the hollow version (and back). */
public class FacadeSwapRecipe extends CustomRecipe {
    public FacadeSwapRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        return !getFacade(inv).isEmpty();
    }

    /** @return The only item in the grid if it is a facade, or an empty stack. */
    private static ItemStack getFacade(CraftingContainer inv) {
        ItemStack found = ItemStack.EMPTY;
        for (int s = 0; s < inv.getContainerSize(); s++) {
            ItemStack stack = inv.getItem(s);
            if (!stack.isEmpty()) {
                if (!found.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                found = stack;
            }
        }
        return found.getItem() instanceof ItemPluggableFacade ? found : ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        ItemStack facade = getFacade(inv);
        if (facade.isEmpty()) {
            return ItemStack.EMPTY;
        }
        FacadeInstance swapped = ItemPluggableFacade.getStates(facade).withSwappedIsHollow();
        return BCSiliconItems.PLUG_FACADE.get().createItemStack(swapped);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BCSiliconRecipeSerializers.FACADE_SWAP.get();
    }
}
