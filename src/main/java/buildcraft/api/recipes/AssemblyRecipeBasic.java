/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.recipes;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** An assembly recipe with a single output, made from a set of ingredients. */
public class AssemblyRecipeBasic extends AssemblyRecipe {
    private final long requiredMicroJoules;
    private final ImmutableSet<IngredientStack> requiredStacks;
    private final ImmutableSet<ItemStack> output;

    public AssemblyRecipeBasic(ResourceLocation name, long requiredMicroJoules, Set<IngredientStack> requiredStacks,
        @Nonnull ItemStack output) {
        super(name);
        this.requiredMicroJoules = requiredMicroJoules;
        this.requiredStacks = ImmutableSet.copyOf(requiredStacks);
        this.output = ImmutableSet.of(output);
    }

    @Override
    public Set<ItemStack> getOutputs(List<ItemStack> inputs) {
        boolean hasAll = requiredStacks.stream().allMatch(definition -> inputs.stream().anyMatch(
            stack -> !stack.isEmpty() && definition.ingredient.test(stack) && stack.getCount() >= definition.count
        ));
        return hasAll ? output : Set.of();
    }

    @Override
    public Set<ItemStack> getOutputPreviews() {
        return output;
    }

    @Override
    public Set<IngredientStack> getInputsFor(@Nonnull ItemStack output) {
        return requiredStacks;
    }

    @Override
    public long getRequiredMicroJoulesFor(@Nonnull ItemStack output) {
        return requiredMicroJoules;
    }
}
