/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.BuildCraft;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryItems;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockWaterGel.GelStage;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class WaterGelGameTests {
    private static final String EMPTY = "gametest/empty5x5x5";

    /** Gel thrown in the middle of a pool spreads to all of its water, then sets once no water is left. The
     * scheduled ticks are run by hand, as they are hundreds of ticks apart. */
    @GameTest(template = EMPTY)
    public static void gelSpreadsThroughAPool(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean edge = x == 0 || x == 4 || z == 0 || z == 4;
                helper.setBlock(new BlockPos(x, 1, z), edge ? Blocks.STONE : Blocks.WATER);
            }
        }
        ServerLevel level = helper.getLevel();
        BlockWaterGel gel = BCFactoryBlocks.WATER_GEL.get();
        gel.startGelling(level, helper.absolutePos(new BlockPos(2, 1, 2)));

        for (int round = 0; round < 100; round++) {
            for (BlockPos pos : pool()) {
                BlockPos absolute = helper.absolutePos(pos);
                BlockState state = level.getBlockState(absolute);
                if (state.is(gel)) {
                    state.tick(level, absolute, level.random);
                }
            }
        }
        for (BlockPos pos : pool()) {
            helper.assertBlockProperty(pos, BlockWaterGel.STAGE, GelStage.GEL);
        }
        helper.succeed();
    }

    /** Set gel drops one gel, and spreading gel one or two. Gel turns back into water in a bucket. */
    @GameTest(template = EMPTY)
    public static void gelDropsAndRecipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState set =
            BCFactoryBlocks.WATER_GEL.get().defaultBlockState().setValue(BlockWaterGel.STAGE, GelStage.GEL);
        List<ItemStack> drops = Block.getDrops(set, level, pos, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(BCFactoryItems.GEL.get())
            && drops.get(0).getCount() == 1, "Set gel should drop one gel, dropped " + drops);

        BlockState spreading = set.setValue(BlockWaterGel.STAGE, GelStage.SPREAD_1);
        int count = Block.getDrops(spreading, level, pos, null).get(0).getCount();
        helper.assertTrue(count >= 1 && count <= 2, "Spreading gel should drop 1 or 2 gel, dropped " + count);

        ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "water_gel_to_bucket");
        helper.assertTrue(level.getRecipeManager().byKey(recipe).isPresent(), "Gel should turn back into water");
        helper.succeed();
    }

    private static List<BlockPos> pool() {
        return BlockPos.betweenClosedStream(1, 1, 1, 3, 1, 3).map(BlockPos::immutable).toList();
    }
}
