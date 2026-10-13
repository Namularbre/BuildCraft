/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.facades;

import javax.annotation.Nullable;

import net.minecraft.world.item.DyeColor;

public interface IFacadePhasedState {
    IFacadeState getState();

    /** @return The pipe signal colour that makes this state active, or null for the default state. */
    @Nullable
    DyeColor getActiveColor();
}
