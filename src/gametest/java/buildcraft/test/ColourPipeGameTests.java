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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeEventActionActivate;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDaizuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourLapis;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.TravellingItem;
import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.tile.TilePipeHolder;

/** Lapis pipes paint items, and daizuli pipes sort them by colour. */
@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class ColourPipeGameTests {
    private static final String TEMPLATE = "gametest/empty5x5x5";
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];

    /** Items that go through a lapis pipe come out with its colour, which a gate action can change. */
    @GameTest(template = TEMPLATE)
    public static void lapisPipesPaintItems(GameTestHelper helper) {
        BlockPos lapisPos = new BlockPos(1, 1, 2);
        helper.setBlock(lapisPos.west(), Blocks.CHEST);
        TilePipeHolder lapis = placePipe(helper, lapisPos, BCTransportItems.PIPE_LAPIS_ITEM.get());
        TilePipeHolder stone = placePipe(helper, lapisPos.east(), BCTransportItems.PIPE_STONE_ITEM.get());
        placePipe(helper, lapisPos.east(2), BCTransportItems.PIPE_STONE_ITEM.get());
        helper.setBlock(lapisPos.east(3), Blocks.CHEST);
        lapis.fireEvent(new PipeEventActionActivate(lapis,
            BCTransportStatements.ACTION_PIPE_COLOUR[DyeColor.RED.ordinal()], NO_PARAMS, EnumPipePart.CENTER));
        helper.assertTrue(((PipeBehaviourLapis) lapis.getPipe().getBehaviour()).getColour() == DyeColor.RED,
            "Gates change the colour");
        helper.runAfterDelay(3, () -> inject(lapis, Direction.WEST, null));
        helper.succeedWhen(() -> {
            PipeFlowItems flow = (PipeFlowItems) stone.getPipe().getFlow();
            boolean red = false;
            for (TravellingItem item : flow.getAllItemsForRender()) {
                red |= item.colour == DyeColor.RED;
            }
            helper.assertTrue(red, "A red item should go through the next pipe");
        });
    }

    /** A daizuli pipe sends the items of its colour where it faces, and the others elsewhere. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void daizuliPipesSortItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos.west(), Blocks.CHEST);
        helper.setBlock(pos.north(), Blocks.CHEST);
        helper.setBlock(pos.east(), Blocks.CHEST);
        TilePipeHolder daizuli = placePipe(helper, pos, BCTransportItems.PIPE_DAIZULI_ITEM.get());
        PipeBehaviourDaizuli behaviour = (PipeBehaviourDaizuli) daizuli.getPipe().getBehaviour();
        behaviour.setColour(DyeColor.BLUE);
        daizuli.fireEvent(new PipeEventActionActivate(daizuli,
            BCTransportStatements.ACTION_PIPE_DIRECTION[Direction.NORTH.ordinal()], NO_PARAMS, EnumPipePart.CENTER));
        helper.runAfterDelay(3, () -> {
            inject(daizuli, Direction.WEST, DyeColor.BLUE);
            inject(daizuli, Direction.WEST, DyeColor.RED);
        });
        helper.succeedWhen(() -> {
            ChestBlockEntity north = (ChestBlockEntity) helper.getBlockEntity(pos.north());
            ChestBlockEntity east = (ChestBlockEntity) helper.getBlockEntity(pos.east());
            helper.assertTrue(north.countItem(Items.COBBLESTONE) == 1, "The blue item goes north");
            helper.assertTrue(east.countItem(Items.COBBLESTONE) == 1, "The red item goes elsewhere");
        });
    }

    private static void inject(TilePipeHolder pipe, Direction from, DyeColor colour) {
        IInjectable injectable = pipe.getCapability(PipeApi.CAP_INJECTABLE, from)
            .orElseThrow(() -> new GameTestAssertException("The pipe should accept items"));
        injectable.injectItem(new ItemStack(Items.COBBLESTONE), true, from, colour, 0.08);
    }

    private static TilePipeHolder placePipe(GameTestHelper helper, BlockPos pos, Item pipeItem) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(pos);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(pipeItem));
        return tile;
    }
}
