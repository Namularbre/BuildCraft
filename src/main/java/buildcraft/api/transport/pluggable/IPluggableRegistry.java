/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.transport.pluggable;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface IPluggableRegistry {
    default void register(PluggableDefinition definition) {
        register(definition.identifier, definition);
    }

    void register(ResourceLocation identifier, PluggableDefinition definition);

    @Nullable
    PluggableDefinition getDefinition(ResourceLocation identifier);
}
