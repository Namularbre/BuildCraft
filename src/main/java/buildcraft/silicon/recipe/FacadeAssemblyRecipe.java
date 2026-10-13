/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.recipe;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipe;
import buildcraft.api.recipes.IngredientStack;

import buildcraft.BuildCraft;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.transport.BCTransportItems;

/** Makes 6 facades (full or hollow) from a block and 3 structure pipes. */
public class FacadeAssemblyRecipe extends AssemblyRecipe {
    public static final FacadeAssemblyRecipe INSTANCE = new FacadeAssemblyRecipe();
    private static final long MJ_COST = 64 * MjAPI.MJ;
    private static final int STRUCTURE_PIPES = 3;

    private FacadeAssemblyRecipe() {
        super(ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "facade"));
    }

    public static ItemStack createFacadeStack(FacadeBlockStateInfo info, boolean isHollow) {
        ItemStack stack = BCSiliconItems.PLUG_FACADE.get().createItemStack(FacadeInstance.createSingle(info, isHollow));
        stack.setCount(6);
        return stack;
    }

    @Override
    public Set<ItemStack> getOutputs(List<ItemStack> inputs) {
        int pipes = 0;
        for (ItemStack stack : inputs) {
            if (stack.is(BCTransportItems.PIPE_STRUCTURE.get())) {
                pipes += stack.getCount();
            }
        }
        Set<ItemStack> outputs = new LinkedHashSet<>();
        if (pipes < STRUCTURE_PIPES) {
            return outputs;
        }
        for (ItemStack stack : inputs) {
            List<FacadeBlockStateInfo> infos = FacadeStateManager.stackFacades.get(stack.getItem());
            if (infos == null) {
                continue;
            }
            for (FacadeBlockStateInfo info : infos) {
                outputs.add(createFacadeStack(info, false));
                outputs.add(createFacadeStack(info, true));
            }
        }
        return outputs;
    }

    @Override
    public Set<ItemStack> getOutputPreviews() {
        FacadeBlockStateInfo stone = FacadeStateManager.getInfoForBlock(Blocks.STONE);
        return stone == null ? Set.of() : Set.of(createFacadeStack(stone, false), createFacadeStack(stone, true));
    }

    @Override
    public Set<IngredientStack> getInputsFor(@Nonnull ItemStack output) {
        ItemStack required = ItemPluggableFacade.getStates(output).phasedStates[0].stateInfo.requiredStack;
        return Set.of(IngredientStack.of(required.getItem()),
            new IngredientStack(Ingredient.of(BCTransportItems.PIPE_STRUCTURE.get()), STRUCTURE_PIPES));
    }

    @Override
    public long getRequiredMicroJoulesFor(@Nonnull ItemStack output) {
        return MJ_COST;
    }
}
