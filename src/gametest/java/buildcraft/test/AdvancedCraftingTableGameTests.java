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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.BuildCraft;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class AdvancedCraftingTableGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos TABLE = new BlockPos(1, 1, 1);
    /** The first result slot of the inventory other blocks see (after the 15 material slots). */
    private static final int FIRST_RESULT = 15;

    /** With laser power, the table crafts its blueprint from the materials, and only asks for power when it can. */
    @GameTest(template = EMPTY)
    public static void tableCraftsWithLaserPower(GameTestHelper helper) {
        helper.setBlock(TABLE, BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get());
        TileAdvancedCraftingTable table = (TileAdvancedCraftingTable) helper.getBlockEntity(TABLE);
        IItemHandler inv = table.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP)
            .orElseThrow(() -> new GameTestAssertException("The table should have an inventory"));
        table.invBlueprint.setStackInSlot(0, new ItemStack(Items.OAK_LOG));

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(table.getRequiredLaserPower() == 0, "Without materials, no power is needed");
            ItemHandlerHelper.insertItemStacked(inv, new ItemStack(Items.OAK_LOG, 2), false);
        });
        helper.runAfterDelay(4, () -> helper.onEachTick(
            () -> table.receiveLaserPower(TileAdvancedCraftingTable.POWER_REQ)
        ));
        helper.succeedWhen(() -> {
            int planks = 0;
            for (int slot = FIRST_RESULT; slot < inv.getSlots(); slot++) {
                ItemStack stack = inv.extractItem(slot, 64, true);
                if (stack.is(Items.OAK_PLANKS)) {
                    planks += stack.getCount();
                }
            }
            helper.assertTrue(planks == 8, "2 logs should make 8 planks, made " + planks);
        });
    }
}
