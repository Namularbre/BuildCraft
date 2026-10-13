/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.recipe;

import javax.annotation.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipeSerializers;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadePhasedState;
import buildcraft.transport.BCTransportItems;

/** Two facades and a pipe wire make a phased facade: it looks like the first facade (the first one in the grid), and
 * like the second one while a wire of that colour is powered in its pipe. The first facade can already be phased, to
 * add more states to it. */
public class FacadePhasedRecipe extends CustomRecipe {
    public FacadePhasedRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        return getResult(inv) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        FacadeInstance result = getResult(inv);
        return result == null ? ItemStack.EMPTY : BCSiliconItems.PLUG_FACADE.get().createItemStack(result);
    }

    @Nullable
    private static FacadeInstance getResult(CraftingContainer inv) {
        FacadeInstance base = null;
        FacadeInstance added = null;
        DyeColor colour = null;
        for (int s = 0; s < inv.getContainerSize(); s++) {
            ItemStack stack = inv.getItem(s);
            if (stack.isEmpty()) {
                continue;
            }
            DyeColor wire = BCTransportItems.getWireColour(stack.getItem());
            if (wire != null && colour == null) {
                colour = wire;
            } else if (stack.getItem() instanceof ItemPluggableFacade && base == null) {
                base = ItemPluggableFacade.getStates(stack);
            } else if (stack.getItem() instanceof ItemPluggableFacade && added == null) {
                added = ItemPluggableFacade.getStates(stack);
            } else {
                return null;
            }
        }
        if (base == null || added == null || colour == null || added.phasedStates.length != 1) {
            return null;
        }
        return base.withState(new FacadePhasedState(added.phasedStates[0].stateInfo, colour));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BCSiliconRecipeSerializers.FACADE_PHASED.get();
    }
}
