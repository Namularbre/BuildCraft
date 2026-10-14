/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.filler.IFillerPatternShape;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.lib.misc.data.Box;

import buildcraft.BuildCraft;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.filler.BCBuildersStatements;
import buildcraft.builders.filler.Template;
import buildcraft.builders.tile.TileFiller;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FillerGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final String BIG = "gametest/empty5x5x5";
    private static final BlockPos FILLER = new BlockPos(0, 1, 0);
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];

    /** The shapes of a few patterns, on small boxes. */
    @GameTest(template = EMPTY)
    public static void patternsMakeTheRightShapes(GameTestHelper helper) {
        helper.assertTrue(count(BCBuildersStatements.PATTERN_FILL, 3, 3, 3) == 27, "Fill fills everything");
        helper.assertTrue(count(BCBuildersStatements.PATTERN_CLEAR, 3, 3, 3) == 0, "Clear fills nothing");
        helper.assertTrue(count(BCBuildersStatements.PATTERN_BOX, 3, 3, 3) == 26, "A box is hollow");
        helper.assertTrue(count(BCBuildersStatements.PATTERN_FRAME, 4, 4, 4) == 32, "A frame is only the edges");
        int pyramid = count(BCBuildersStatements.PATTERN_PYRAMID, 5, 3, 5);
        helper.assertTrue(pyramid == 25 + 9 + 1, "A pyramid gets smaller as it goes up, got " + pyramid);

        Template sphere = template(BCBuildersStatements.PATTERN_SPHERE, 7, 7, 7);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                for (int z = 0; z < 7; z++) {
                    helper.assertTrue(sphere.get(x, y, z) == sphere.get(6 - x, 6 - y, 6 - z), "A sphere is symmetric");
                }
            }
        }
        helper.assertTrue(sphere.get(3, 3, 3) && !sphere.get(0, 0, 0), "A sphere is round");
        helper.succeed();
    }

    /** With power and blocks, the filler fills its box. */
    @GameTest(template = BIG, timeoutTicks = 400)
    public static void fillersFillBoxes(GameTestHelper helper) {
        TileFiller filler = placeFiller(helper, new BlockPos(2, 1, 2), new BlockPos(4, 1, 4));
        filler.invResources.insertItem(0, new ItemStack(Items.COBBLESTONE, 16), false);
        filler.setPattern(BCBuildersStatements.PATTERN_FILL, NO_PARAMS);
        helper.succeedWhen(() -> {
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(x, 1, z));
                }
            }
            helper.assertTrue(filler.invResources.getStackInSlot(0).getCount() == 16 - 9, "9 blocks are used");
        });
    }

    /** "Clear" breaks the blocks of the box (if the filler can excavate). */
    @GameTest(template = BIG, timeoutTicks = 400)
    public static void fillersClearBoxes(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.DIRT);
        }
        TileFiller filler = placeFiller(helper, new BlockPos(2, 1, 3), new BlockPos(4, 1, 3));
        filler.setPattern(BCBuildersStatements.PATTERN_CLEAR, NO_PARAMS);
        helper.succeedWhen(() -> {
            for (int x = 2; x <= 4; x++) {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 1, 3));
            }
        });
    }

    /** An octagon is a square with its corners cut off. */
    @GameTest(template = EMPTY)
    public static void octagonsCutCorners(GameTestHelper helper) {
        Template octagon = template(BCBuildersStatements.PATTERN_OCTAGON, 7, 1, 7);
        helper.assertTrue(octagon.get(3, 0, 0) && octagon.get(0, 0, 3), "The sides are drawn");
        helper.assertTrue(!octagon.get(0, 0, 0) && !octagon.get(6, 0, 6), "The corners are cut");
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.assertTrue(octagon.get(x, 0, z) == octagon.get(6 - x, 0, 6 - z), "An octagon is symmetric");
            }
        }
        helper.succeed();
    }

    /** "Horizon" clears everything above the bottom of the box (up to the sky, so outside of the box too), and fills
     * the layer under it. (Only the template is checked, as there is a lot of ground above the test.) */
    @GameTest(template = EMPTY)
    public static void horizonsGoUpToTheSky(GameTestHelper helper) {
        TileFiller filler = placeFiller(helper, new BlockPos(1, 1, 1), new BlockPos(2, 2, 2));
        Template horizon = (Template) BCBuildersStatements.PATTERN_HORIZON.createTemplate(filler, NO_PARAMS);
        BlockPos boxMin = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(horizon != null && horizon.min != null && horizon.min.getY() == boxMin.getY() - 1,
            "The template should start under the box");
        int height = horizon.getSize().getY();
        helper.assertTrue(horizon.min.getY() + height == helper.getLevel().getMaxBuildHeight(),
            "The template should go up to the sky");
        helper.assertTrue(horizon.get(0, 0, 0) && horizon.get(1, 0, 1), "The layer under the box is filled");
        helper.assertTrue(!horizon.get(0, 1, 0) && !horizon.get(1, height - 1, 1), "Everything above is cleared");
        helper.succeed();
    }

    /** "Flatten" clears the box, and fills the holes under it down to the ground. */
    @GameTest(template = BIG, timeoutTicks = 400)
    public static void flattenFillsHolesUnderTheBox(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.DIRT);
        TileFiller filler = placeFiller(helper, new BlockPos(2, 3, 2), new BlockPos(4, 3, 4));
        filler.invResources.insertItem(0, new ItemStack(Items.COBBLESTONE, 32), false);
        filler.setPattern(BCBuildersStatements.PATTERN_FLATTEN, NO_PARAMS);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 3, 3));
            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(x, 1, z));
                    helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(x, 2, z));
                }
            }
        });
    }

    /** A gate next to the filler can set its pattern. */
    @GameTest(template = EMPTY)
    public static void gatesSetFillerPatterns(GameTestHelper helper) {
        TileFiller filler = placeFiller(helper, new BlockPos(1, 1, 1), new BlockPos(2, 2, 2));
        BCBuildersStatements.PATTERN_BOX.actionActivate(filler, Direction.UP, null, NO_PARAMS);
        helper.assertTrue(filler.patternStatement.get() == BCBuildersStatements.PATTERN_BOX,
            "The pattern should be set");
        helper.assertTrue(filler.isLocked(), "The GUI shows that a gate set it");
        helper.succeed();
    }

    private static int count(IFillerPatternShape pattern, int x, int y, int z) {
        return template(pattern, x, y, z).data.cardinality();
    }

    private static Template template(IFillerPatternShape pattern, int x, int y, int z) {
        Template template = new Template(new BlockPos(x, y, z));
        IStatementParameter[] params = new IStatementParameter[pattern.maxParameters()];
        for (int i = 0; i < params.length; i++) {
            params[i] = pattern.createParameter(i);
        }
        pattern.fillTemplate(template, params);
        return template;
    }

    private static TileFiller placeFiller(GameTestHelper helper, BlockPos min, BlockPos max) {
        helper.setBlock(FILLER, BCBuildersBlocks.FILLER.get());
        TileFiller filler = (TileFiller) helper.getBlockEntity(FILLER);
        filler.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCBuildersBlocks.FILLER.get()));
        filler.setBox(new Box(helper.absolutePos(min), helper.absolutePos(max)));
        filler.getBattery().addPower(16_000 * MjAPI.MJ, false);
        return filler;
    }
}
