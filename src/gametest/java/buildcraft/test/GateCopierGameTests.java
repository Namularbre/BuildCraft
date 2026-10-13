/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.statements.IStatementParameter;

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
import buildcraft.silicon.item.ItemGateCopier;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class GateCopierGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";
    private static final IStatementParameter[] NO_PARAMS = new IStatementParameter[0];

    /** Clicking a gate with an empty copier copies it, and clicking another gate pastes it there. */
    @GameTest(template = EMPTY)
    public static void gateCopiersCopyAndPaste(GameTestHelper helper) {
        PluggableGate from = placeGate(helper, new BlockPos(0, 1, 1), EnumGateLogic.AND);
        PluggableGate to = placeGate(helper, new BlockPos(2, 1, 1), EnumGateLogic.OR);
        Player player = helper.makeMockPlayer();
        ItemStack copier = new ItemStack(BCSiliconItems.GATE_COPIER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, copier);

        from.onPluggableActivate(player, null, 0, 0, 0);
        helper.assertTrue(ItemGateCopier.getCopiedGateData(copier) == null, "An empty gate has nothing to copy");

        from.logic.setTrigger(0, new TriggerWrapperInternal(BCCoreStatements.TRIGGER_TRUE), NO_PARAMS);
        from.logic.setAction(0, new ActionWrapperInternal(BCCoreStatements.ACTION_REDSTONE), NO_PARAMS);
        from.logic.setConnected(0, true);
        from.onPluggableActivate(player, null, 0, 0, 0);
        helper.assertTrue(ItemGateCopier.getCopiedGateData(copier) != null, "The gate should be copied");

        to.onPluggableActivate(player, null, 0, 0, 0);
        helper.assertTrue(to.logic.statements[0].trigger.get() != null
            && to.logic.statements[0].trigger.get().delegate == BCCoreStatements.TRIGGER_TRUE, "The trigger is pasted");
        helper.assertTrue(to.logic.statements[0].action.get() != null, "The action is pasted");
        helper.assertTrue(to.logic.connections[0], "The groups are pasted");

        List<Component> warnings = to.logic.pasteConfig(ItemGateCopier.getCopiedGateData(copier));
        helper.assertTrue(warnings.size() == 1, "The logic of the gates is different, got " + warnings);
        helper.succeed();
    }

    private static PluggableGate placeGate(GameTestHelper helper, BlockPos pos, EnumGateLogic logic) {
        helper.setBlock(pos, BCTransportBlocks.PIPE_HOLDER.get());
        TilePipeHolder pipe = (TilePipeHolder) helper.getBlockEntity(pos);
        pipe.onPlacedBy(helper.makeMockPlayer(), new ItemStack(BCTransportItems.PIPE_STONE_ITEM.get()));
        GateVariant variant = new GateVariant(logic, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);
        PluggableGate gate = new PluggableGate(BCSiliconPlugs.GATE, pipe, Direction.UP, variant);
        pipe.replacePluggable(Direction.UP, gate);
        return gate;
    }
}
