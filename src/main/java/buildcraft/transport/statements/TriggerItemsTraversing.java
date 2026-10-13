/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementParameterItemStack;

import buildcraft.core.statements.BCStatement;
import buildcraft.transport.pipe.flow.PipeFlowItems;

/** Active when items (or the given item) go through the pipe. */
public class TriggerItemsTraversing extends BCStatement implements ITriggerInternal {
    public TriggerItemsTraversing() {
        super("buildcraft:pipe_contains_items");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.pipe.containsItems");
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_pipecontents_containsitems");
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return StatementParameterItemStack.EMPTY;
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IGate gate && gate.getPipeHolder().getPipe().getFlow() instanceof PipeFlowItems flow) {
            ItemStack filter = getParam(0, parameters, StatementParameterItemStack.EMPTY).getItemStack();
            return flow.containsItemMatching(filter);
        }
        return false;
    }
}
