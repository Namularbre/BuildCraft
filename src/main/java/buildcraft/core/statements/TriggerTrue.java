/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerInternal;

/** Always active. */
public class TriggerTrue extends BCStatement implements ITriggerInternal {
    public TriggerTrue() {
        super("buildcraftcore:trigger.true");
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_true");
    }

    @Override
    public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
        return true;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.true");
    }
}
