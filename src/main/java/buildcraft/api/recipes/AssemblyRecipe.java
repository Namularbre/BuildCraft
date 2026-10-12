/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.recipes;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A recipe of the assembly table: some items, and MJ from lasers, make an item. */
public abstract class AssemblyRecipe implements Comparable<AssemblyRecipe> {
    private final ResourceLocation name;

    protected AssemblyRecipe(ResourceLocation name) {
        this.name = name;
    }

    public ResourceLocation getRegistryName() {
        return name;
    }

    /** @param inputs Current ingredients in the assembly table
     * @return Every output that can be made from the given inputs (empty if nothing can be). */
    public abstract Set<ItemStack> getOutputs(List<ItemStack> inputs);

    /** @return Every output of this recipe, for previews (like JEI). */
    public abstract Set<ItemStack> getOutputPreviews();

    /** @param output An output given by {@link #getOutputs(List)} or {@link #getOutputPreviews()}
     * @return The items used up to make the given output. */
    public abstract Set<IngredientStack> getInputsFor(@Nonnull ItemStack output);

    /** @param output An output given by {@link #getOutputs(List)} or {@link #getOutputPreviews()}
     * @return How many micro MJ it takes to make the given output. */
    public abstract long getRequiredMicroJoulesFor(@Nonnull ItemStack output);

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof AssemblyRecipe other && name.equals(other.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(AssemblyRecipe o) {
        return name.compareTo(o.name);
    }
}
