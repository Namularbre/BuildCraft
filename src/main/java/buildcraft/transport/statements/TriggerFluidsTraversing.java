/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;

import buildcraft.core.statements.BCStatement;
import buildcraft.transport.pipe.flow.PipeFlowFluids;

/** Active when fluid goes through the pipe. */
public class TriggerFluidsTraversing extends BCStatement implements ITriggerInternal {
    public TriggerFluidsTraversing() {
        super("buildcraft:pipe_contains_fluids");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.pipe.containsFluids");
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_pipecontents_containsfluids");
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        return source instanceof IGate gate && gate.getPipeHolder().getPipe().getFlow() instanceof PipeFlowFluids flow
            && flow.doesContainFluid();
    }
}
