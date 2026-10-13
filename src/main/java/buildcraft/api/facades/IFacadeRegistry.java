/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.facades;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;

public interface IFacadeRegistry {
    Collection<? extends IFacadeState> getValidFacades();

    IFacadePhasedState createPhasedState(IFacadeState state, @Nullable DyeColor activeColor);

    IFacade createPhasedFacade(IFacadePhasedState[] states, boolean isHollow);

    default IFacade createBasicFacade(IFacadeState state, boolean isHollow) {
        return createPhasedFacade(new IFacadePhasedState[] { createPhasedState(state, null) }, isHollow);
    }
}
