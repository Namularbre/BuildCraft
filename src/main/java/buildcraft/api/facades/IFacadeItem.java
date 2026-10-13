/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.facades;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface IFacadeItem {
    @Nullable
    default FacadeType getFacadeType(@Nonnull ItemStack stack) {
        IFacade facade = getFacade(stack);
        return facade == null ? null : facade.getType();
    }

    /** @return A facade that looks like the given state, or an empty stack if that state can't be a facade. */
    @Nonnull
    ItemStack getFacadeForBlock(BlockState state);

    /** @param facade The facade. NOTE: This MUST be an object returned from
     *            {@link IFacadeRegistry#createBasicFacade(IFacadeState, boolean)} or
     *            {@link IFacadeRegistry#createPhasedFacade(IFacadePhasedState[], boolean)}, otherwise a
     *            {@link ClassCastException} will be thrown! */
    ItemStack createFacadeStack(IFacade facade);

    @Nullable
    IFacade getFacade(@Nonnull ItemStack facade);
}
