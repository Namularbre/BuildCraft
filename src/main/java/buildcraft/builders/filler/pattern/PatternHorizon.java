/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler.pattern;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;

import buildcraft.builders.filler.Template;

/** Clears everything above the bottom of the box (up to the sky), and fills the layer just under it: this makes a flat
 * floor with nothing above it. (From BuildCraft 7: it goes outside of the box.) */
public class PatternHorizon extends Pattern {
    public PatternHorizon() {
        super("horizon");
    }

    @Nullable
    @Override
    public IFilledTemplate createTemplate(IFillerStatementContainer filler, IStatementParameter[] params) {
        Level level = filler.getFillerWorld();
        BlockPos boxMin = filler.getBoxMin();
        BlockPos boxSize = filler.getBoxSize();
        boolean hasFloor = boxMin.getY() > level.getMinBuildHeight();
        int bottom = hasFloor ? boxMin.getY() - 1 : boxMin.getY();
        int top = level.getMaxBuildHeight() - 1;
        if (top < bottom) {
            return null;
        }
        BlockPos min = new BlockPos(boxMin.getX(), bottom, boxMin.getZ());
        Template template = new Template(min, new BlockPos(boxSize.getX(), top - bottom + 1, boxSize.getZ()));
        if (hasFloor) {
            for (int x = 0; x < boxSize.getX(); x++) {
                template.setLineZ(x, 0, 0, boxSize.getZ() - 1, true);
            }
        }
        return template;
    }
}
