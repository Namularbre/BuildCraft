/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.recipes;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** An ingredient, and how many items of it are needed. */
public final class IngredientStack {
    public final Ingredient ingredient;
    public final int count;

    public IngredientStack(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public IngredientStack(Ingredient ingredient) {
        this(ingredient, 1);
    }

    public static IngredientStack of(ItemLike item) {
        return new IngredientStack(Ingredient.of(item));
    }

    public static IngredientStack of(TagKey<Item> tag) {
        return new IngredientStack(Ingredient.of(tag));
    }

    public static IngredientStack of(ItemStack stack) {
        return new IngredientStack(Ingredient.of(stack), stack.getCount());
    }
}
