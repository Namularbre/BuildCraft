/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.facades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A block state that facades can look like. */
public interface IFacadeState {
    boolean isTransparent();

    BlockState getBlockState();

    /** @return The item used to make the facade (empty if it can't be made). */
    ItemStack getRequiredStack();
}
