/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler.pattern;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.filler.FillerManager;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;

import buildcraft.builders.filler.BCBuildersStatements;
import buildcraft.builders.filler.FillerSprites;
import buildcraft.core.statements.BCStatement;

/** A filler pattern. Patterns are also actions: a gate next to a filler can change its pattern. */
public abstract class Pattern extends BCStatement implements IFillerPattern, IActionExternal {
    private final String tag;

    public Pattern(String tag) {
        super("buildcraft:" + tag);
        this.tag = tag;
        FillerManager.registry.addPattern(this);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.fillerpattern." + tag);
    }

    @Override
    public ResourceLocation getSprite() {
        return FillerSprites.pattern(tag);
    }

    @Override
    public void actionActivate(BlockEntity target, Direction side, IStatementContainer source,
        IStatementParameter[] parameters) {
        if (source instanceof IFillerStatementContainer filler) {
            filler.setPattern(this, parameters);
        } else if (target instanceof IFillerStatementContainer filler) {
            filler.setPattern(this, parameters);
        }
    }

    @Override
    public IFillerPattern[] getPossible() {
        return BCBuildersStatements.PATTERNS;
    }
}
