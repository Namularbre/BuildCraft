/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler.pattern;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.containers.IFillerStatementContainer;

import buildcraft.builders.filler.Template;

/** Clears the box, and fills every hole under it (down to the ground of each column): this flattens the land at the
 * bottom of the box. (From BuildCraft 7: it goes outside of the box.) */
public class PatternFlatten extends Pattern {
    public PatternFlatten() {
        super("flatten");
    }

    @Nullable
    @Override
    public IFilledTemplate createTemplate(IFillerStatementContainer filler, IStatementParameter[] params) {
        Level level = filler.getFillerWorld();
        BlockPos boxMin = filler.getBoxMin();
        BlockPos boxSize = filler.getBoxSize();
        // The lowest ground under the box: everything above it (and below the box) is filled
        int bottom = boxMin.getY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < boxSize.getX(); x++) {
            for (int z = 0; z < boxSize.getZ(); z++) {
                int y = boxMin.getY() - 1;
                while (y >= level.getMinBuildHeight()) {
                    BlockState state = level.getBlockState(pos.set(boxMin.getX() + x, y, boxMin.getZ() + z));
                    if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                        break;
                    }
                    y--;
                }
                bottom = Math.min(bottom, y + 1);
            }
        }
        BlockPos min = new BlockPos(boxMin.getX(), bottom, boxMin.getZ());
        int height = boxMin.getY() - bottom + boxSize.getY();
        Template template = new Template(min, new BlockPos(boxSize.getX(), height, boxSize.getZ()));
        // Blocks that are already there count as filled, so only the holes are filled
        for (int y = 0; y < boxMin.getY() - bottom; y++) {
            for (int x = 0; x < boxSize.getX(); x++) {
                template.setLineZ(x, y, 0, boxSize.getZ() - 1, true);
            }
        }
        return template;
    }
}
