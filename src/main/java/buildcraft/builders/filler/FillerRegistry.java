/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;

import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.filler.IFillerRegistry;

public enum FillerRegistry implements IFillerRegistry {
    INSTANCE;

    private final Map<String, IFillerPattern> patterns = new LinkedHashMap<>();

    @Override
    public void addPattern(IFillerPattern pattern) {
        patterns.put(pattern.getUniqueTag(), pattern);
    }

    @Nullable
    @Override
    public IFillerPattern getPattern(String name) {
        return patterns.get(name);
    }

    @Override
    public Collection<IFillerPattern> getPatterns() {
        return Collections.unmodifiableCollection(patterns.values());
    }

    @Override
    public IFilledTemplate createFilledTemplate(BlockPos pos, BlockPos size) {
        return new Template(size);
    }
}
