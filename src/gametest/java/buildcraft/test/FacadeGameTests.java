/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.transport.EnumWirePart;

import buildcraft.lib.statement.ActionWrapper.ActionWrapperInternal;
import buildcraft.lib.statement.TriggerWrapper.TriggerWrapperInternal;

import buildcraft.BuildCraft;
import buildcraft.core.statements.BCCoreStatements;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.silicon.plug.PluggableFacade;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.recipe.FacadeAssemblyRecipe;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class FacadeGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final BlockPos PIPE = new BlockPos(1, 1, 1);

    /** Facades can look like full, plain blocks, but not like fluids or blocks with a block entity. */
    @GameTest(template = EMPTY)
    public static void facadesOnlyLookLikeFullBlocks(GameTestHelper helper) {
        helper.assertTrue(FacadeStateManager.getInfoForBlock(Blocks.STONE) != null, "Stone should be valid");
        helper.assertTrue(FacadeStateManager.getInfoForBlock(Blocks.GLASS) != null, "Glass should be valid");
        helper.assertTrue(FacadeStateManager.getInfoForBlock(Blocks.CHEST) == null, "Chests have a block entity");
        helper.assertTrue(FacadeStateManager.getInfoForBlock(Blocks.WATER) == null, "Water is a fluid");
        helper.assertTrue(FacadeStateManager.getInfoForBlock(Blocks.OAK_SLAB) == null, "Slabs aren't full");
        helper.assertTrue(FacadeStateManager.stackFacades.get(Items.OAK_LOG).size() == 3,
            "Logs should give a facade for each axis");
        helper.succeed();
    }

    /** The assembly table makes 6 facades (full or hollow) from a block and 3 structure pipes. */
    @GameTest(template = EMPTY)
    public static void facadesAreAssembled(GameTestHelper helper) {
        ItemStack pipes = new ItemStack(BCTransportItems.PIPE_STRUCTURE.get(), 3);
        Set<ItemStack> outputs = FacadeAssemblyRecipe.INSTANCE.getOutputs(List.of(pipes, new ItemStack(Items.STONE)));
        helper.assertTrue(outputs.size() == 2, "Stone should make a full and a hollow facade, made " + outputs);
        for (ItemStack output : outputs) {
            helper.assertTrue(output.getCount() == 6, "6 facades at a time");
            helper.assertTrue(ItemPluggableFacade.getStates(output).phasedStates[0].stateInfo.state
                == Blocks.STONE.defaultBlockState(), "The facades should look like stone");
            helper.assertTrue(FacadeAssemblyRecipe.INSTANCE.getInputsFor(output).size() == 2, "Stone and pipes");
        }
        Set<ItemStack> withoutPipes = FacadeAssemblyRecipe.INSTANCE.getOutputs(List.of(new ItemStack(Items.STONE)));
        helper.assertTrue(withoutPipes.isEmpty(), "Structure pipes are needed");
        helper.succeed();
    }

    /** A facade alone in a crafting grid becomes hollow (and back). */
    @GameTest(template = EMPTY)
    public static void facadesSwapHollowInCraftingGrids(GameTestHelper helper) {
        ItemStack facade = stoneFacade(false);
        TransientCraftingContainer grid = new TransientCraftingContainer(dummyMenu(), 3, 3);
        grid.setItem(4, facade);
        ItemStack result = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid,
            helper.getLevel()).map(r -> r.assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        helper.assertTrue(result.getItem() instanceof ItemPluggableFacade,
            "The facade should be crafted, got " + result);
        helper.assertTrue(ItemPluggableFacade.getStates(result).isHollow, "It should be hollow now");
        helper.succeed();
    }

    /** A full facade stops the pipe from connecting on its side, a hollow one doesn't. Facades are saved and
     * dropped. */
    @GameTest(template = EMPTY)
    public static void facadesCoverPipes(GameTestHelper helper) {
        helper.setBlock(PIPE.east(), Blocks.CHEST);
        helper.setBlock(PIPE.west(), Blocks.CHEST);
        TilePipeHolder pipe = placePipe(helper);
        place(helper, pipe, Direction.EAST, stoneFacade(false));
        place(helper, pipe, Direction.WEST, stoneFacade(true));
        helper.assertTrue(pipe.getPluggable(Direction.EAST) instanceof PluggableFacade, "The facade should be placed");

        CompoundTag saved = pipe.saveWithoutMetadata();
        pipe.replacePluggable(Direction.EAST, null);
        pipe.load(saved);
        helper.assertTrue(pipe.getPluggable(Direction.EAST) instanceof PluggableFacade facade
            && facade.getActiveBlockState() == Blocks.STONE.defaultBlockState(), "The facade should be saved");

        NonNullList<ItemStack> drops = NonNullList.create();
        pipe.addDrops(drops);
        helper.assertTrue(drops.stream().filter(s -> s.getItem() instanceof ItemPluggableFacade).count() == 2,
            "Both facades should drop, dropped " + drops);
        helper.succeedWhen(() -> {
            helper.assertTrue(!pipe.getPipe().isConnected(Direction.EAST), "A full facade blocks the pipe");
            helper.assertTrue(pipe.getPipe().isConnected(Direction.WEST), "A hollow facade doesn't");
        });
    }

    /** Two facades and a wire make a phased facade, which looks like the second facade while the wire is powered. */
    @GameTest(template = EMPTY)
    public static void phasedFacadesFollowWires(GameTestHelper helper) {
        TransientCraftingContainer grid = new TransientCraftingContainer(dummyMenu(), 3, 3);
        grid.setItem(0, stoneFacade(false));
        grid.setItem(1, facade(Blocks.GLASS));
        grid.setItem(2, new ItemStack(BCTransportItems.wire(DyeColor.RED)));
        ItemStack phased = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid,
            helper.getLevel()).map(r -> r.assemble(grid, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        FacadeInstance states = ItemPluggableFacade.getStates(phased);
        helper.assertTrue(states.phasedStates.length == 2 && states.phasedStates[1].activeColour == DyeColor.RED,
            "The facade should be phased, got " + phased);
        grid.setItem(0, phased);
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid,
            helper.getLevel()).isEmpty(), "There already is a red state");

        TilePipeHolder pipe = placePipe(helper);
        pipe.wireManager.addPart(EnumWirePart.EAST_UP_SOUTH, DyeColor.RED);
        place(helper, pipe, Direction.EAST, phased);
        PluggableFacade facade = (PluggableFacade) pipe.getPluggable(Direction.EAST);
        PluggableGate gate = new PluggableGate(BCSiliconPlugs.GATE, pipe, Direction.NORTH,
            new GateVariant(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER));
        pipe.replacePluggable(Direction.NORTH, gate);
        gate.logic.setAction(0, new ActionWrapperInternal(
            BCTransportStatements.ACTION_PIPE_SIGNAL[DyeColor.RED.ordinal()]
        ), new IStatementParameter[0]);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(facade.getActiveBlockState() == Blocks.STONE.defaultBlockState(), "No signal yet");
            gate.logic.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE),
                new IStatementParameter[0]);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(facade.getActiveBlockState() == Blocks.GLASS.defaultBlockState(),
                "The facade should look like glass while the wire is powered");
            helper.succeed();
        });
    }

    private static AbstractContainerMenu dummyMenu() {
        return new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
    }

    private static ItemStack facade(Block block) {
        return BCSiliconItems.PLUG_FACADE.get().createItemStack(
            FacadeInstance.createSingle(FacadeStateManager.getInfoForBlock(block), false)
        );
    }

    private static ItemStack stoneFacade(boolean hollow) {
        return BCSiliconItems.PLUG_FACADE.get().createItemStack(
            FacadeInstance.createSingle(FacadeStateManager.getInfoForBlock(Blocks.STONE), hollow)
        );
    }

    private static TilePipeHolder placePipe(GameTestHelper helper) {
        helper.setBlock(PIPE, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder tile = (TilePipeHolder) helper.getBlockEntity(PIPE);
        tile.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get()));
        return tile;
    }

    private static void place(GameTestHelper helper, TilePipeHolder pipe, Direction side, ItemStack stack) {
        Item item = stack.getItem();
        var plug = ((ItemPluggableFacade) item).onPlace(stack, pipe, side, helper.makeMockPlayer(),
            InteractionHand.MAIN_HAND);
        helper.assertTrue(plug != null, "The facade should be placeable");
        pipe.replacePluggable(side, plug);
    }
}
