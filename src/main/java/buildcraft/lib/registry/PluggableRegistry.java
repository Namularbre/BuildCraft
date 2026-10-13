/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.registry;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pluggable.IPluggableRegistry;
import buildcraft.api.transport.pluggable.PluggableDefinition;

public enum PluggableRegistry implements IPluggableRegistry {
    INSTANCE;

    private final Map<ResourceLocation, PluggableDefinition> definitions = new HashMap<>();

    @Override
    public void register(ResourceLocation identifier, PluggableDefinition definition) {
        definitions.put(identifier, definition);
    }

    @Nullable
    @Override
    public PluggableDefinition getDefinition(ResourceLocation identifier) {
        return definitions.get(identifier);
    }
}
