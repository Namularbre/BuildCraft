/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.core.statements.BCStatement;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;

/** Activates one of the filters of an emzuli pipe (while the action is active). The pipe does it when it sees the
 * action. */
public class ActionExtractionPreset extends BCStatement implements IActionInternal {
    public final SlotIndex index;

    public ActionExtractionPreset(SlotIndex index) {
        super("buildcraft:extraction.preset." + index.colour.getName(),
            "buildcraft.extraction.preset." + index.colour.getName());
        this.index = index;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.action.extraction",
            Component.translatable("color.minecraft." + index.colour.getName()));
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        // The pipe handles this
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("extraction_preset_" + index.colour.getName());
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.ACTION_EXTRACTION_PRESET;
    }
}
