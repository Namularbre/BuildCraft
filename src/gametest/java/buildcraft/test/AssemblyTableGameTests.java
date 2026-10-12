/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.BuildCraft;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.EnumRedstoneChipset;
import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable.AssemblyInstruction;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class AssemblyTableGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos TABLE = new BlockPos(1, 1, 1);

    /** Redstone in the table allows the redstone chipset: once asked for, and with enough power, it is made and
     * sent to the chest next to the table. */
    @GameTest(template = EMPTY)
    public static void tableMakesSavedRecipes(GameTestHelper helper) {
        TileAssemblyTable table = placeTable(helper);
        helper.setBlock(TABLE.east(), Blocks.CHEST);
        table.inv.setStackInSlot(0, new ItemStack(Items.REDSTONE));

        helper.runAfterDelay(2, () -> {
            AssemblyInstruction redChipset = findRecipe(helper, table, EnumRedstoneChipset.RED);
            helper.assertTrue(table.recipesStates.get(redChipset) == EnumAssemblyRecipeState.POSSIBLE,
                "The recipe should be possible, but not asked for");
            helper.assertTrue(table.getTarget() == 0, "Nothing should be made until a recipe is asked for");
            table.setRecipeState(redChipset, EnumAssemblyRecipeState.SAVED);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(table.getTarget() == 10_000 * MjAPI.MJ, "The chipset takes 10 000 MJ");
            long excess = table.receiveLaserPower(20_000 * MjAPI.MJ);
            helper.assertTrue(excess == 10_000 * MjAPI.MJ, "The table should only take what it needs");
        });
        helper.succeedWhen(() -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(TABLE.east());
            Item chipset = BCSiliconItems.chipset(EnumRedstoneChipset.RED);
            helper.assertTrue(chest.countItem(chipset) == 1, "The chipset should be in the chest");
            helper.assertTrue(table.inv.getStackInSlot(0).isEmpty(), "The redstone should be used up");
        });
    }

    /** A laser below a table that needs power sends it power. */
    @GameTest(template = EMPTY)
    public static void laserPowersTables(GameTestHelper helper) {
        helper.setBlock(TABLE.below(), BCSiliconBlocks.LASER.get().defaultBlockState()
            .setValue(BlockLaser.FACING, Direction.UP));
        TileAssemblyTable table = placeTable(helper);
        table.inv.setStackInSlot(0, new ItemStack(Items.REDSTONE));
        IMjReceiver laser = helper.getBlockEntity(TABLE.below()).getCapability(MjAPI.CAP_RECEIVER, Direction.DOWN)
            .orElseThrow(() -> new GameTestAssertException("The laser should accept power"));
        helper.onEachTick(() -> laser.receivePower(100 * MjAPI.MJ, false));
        helper.runAfterDelay(2, () -> table.setRecipeState(
            findRecipe(helper, table, EnumRedstoneChipset.RED), EnumAssemblyRecipeState.SAVED
        ));
        helper.succeedWhen(() -> helper.assertTrue(table.power > 0, "The laser should power the table"));
    }

    private static TileAssemblyTable placeTable(GameTestHelper helper) {
        helper.setBlock(TABLE, BCSiliconBlocks.ASSEMBLY_TABLE.get());
        return (TileAssemblyTable) helper.getBlockEntity(TABLE);
    }

    private static AssemblyInstruction findRecipe(GameTestHelper helper, TileAssemblyTable table,
        EnumRedstoneChipset chipset) {
        for (AssemblyInstruction instruction : table.recipesStates.keySet()) {
            if (instruction.output().is(BCSiliconItems.chipset(chipset))) {
                return instruction;
            }
        }
        throw new GameTestAssertException("No recipe for the " + chipset + " chipset");
    }
}
