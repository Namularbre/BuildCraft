/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.pipe.PipeEventActionActivate;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.tile.TilePipeHolder;

/** Diamond wooden pipes and emzuli pipes: wooden pipes that only extract what their filters allow. A chest (with
 * cobblestone and dirt) is west of the pipe, and the items go east to another chest. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FilteredWoodPipeGameTests {
    private static final String TEMPLATE = "gametest/empty5x5x5";
    private static final BlockPos PIPE = new BlockPos(1, 1, 2);

    /** With a whitelist, only the items in the filters are extracted. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void diamondWoodPipesOnlyExtractFilteredItems(GameTestHelper helper) {
        TilePipeHolder pipe = setUp(helper, BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get());
        PipeBehaviourWoodDiamond behaviour = (PipeBehaviourWoodDiamond) pipe.getPipe().getBehaviour();
        behaviour.filters.setStackInSlot(4, new ItemStack(Items.DIRT));
        helper.runAfterDelay(3, () -> {
            for (int i = 0; i < 4; i++) {
                behaviour.receivePower(MjAPI.MJ, false);
            }
        });
        helper.succeedWhen(() -> {
            ChestBlockEntity target = (ChestBlockEntity) helper.getBlockEntity(PIPE.east(3));
            helper.assertTrue(target.countItem(Items.DIRT) == 4, "4 dirt should be extracted");
            helper.assertTrue(target.countItem(Items.COBBLESTONE) == 0, "Cobblestone isn't in the filters");
        });
    }

    /** An emzuli pipe only extracts while a gate activates one of its filters, and paints what it extracts. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void emzuliPipesExtractWithGates(GameTestHelper helper) {
        TilePipeHolder pipe = setUp(helper, BCTransportItems.PIPE_EMZULI_ITEM.get());
        PipeBehaviourEmzuli behaviour = (PipeBehaviourEmzuli) pipe.getPipe().getBehaviour();
        behaviour.invFilters.setStackInSlot(SlotIndex.TRIANGLE.ordinal(), new ItemStack(Items.COBBLESTONE));
        behaviour.setSlotColour(SlotIndex.TRIANGLE, DyeColor.ORANGE);
        TilePipeHolder next = (TilePipeHolder) helper.getBlockEntity(PIPE.east());
        helper.runAfterDelay(3, () -> {
            behaviour.receivePower(MjAPI.MJ, false);
            helper.assertTrue(itemCount(next) == 0 && itemCount(pipe) == 0, "No preset is active");
            pipe.fireEvent(new PipeEventActionActivate(pipe,
                BCTransportStatements.ACTION_EXTRACTION_PRESET[SlotIndex.TRIANGLE.ordinal()],
                new IStatementParameter[0], EnumPipePart.CENTER));
            behaviour.receivePower(MjAPI.MJ, false);
            helper.assertTrue(itemCount(pipe) == 1, "The active preset extracts");
        });
        helper.succeedWhen(() -> {
            boolean orange = false;
            for (TravellingItem item : ((PipeFlowItems) next.getPipe().getFlow()).getAllItemsForRender()) {
                orange |= item.colour == DyeColor.ORANGE && item.getStack().is(Items.COBBLESTONE);
            }
            helper.assertTrue(orange, "The cobblestone should be painted orange");
        });
    }

    private static int itemCount(TilePipeHolder pipe) {
        return ((PipeFlowItems) pipe.getPipe().getFlow()).getAllItemsForRender().size();
    }

    private static TilePipeHolder setUp(GameTestHelper helper, Item pipeItem) {
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        ChestBlockEntity source = (ChestBlockEntity) helper.getBlockEntity(PIPE.west());
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        source.setItem(1, new ItemStack(Items.DIRT, 16));
        TilePipeHolder pipe = placePipe(helper, PIPE, pipeItem);
        placePipe(helper, PIPE.east(), BCTransportItems.PIPE_STONE_ITEM.get());
        placePipe(helper, PIPE.east(2), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(PIPE.east(3), Blocks.CHEST);
        return pipe;
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }
}
