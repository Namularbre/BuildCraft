/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.core.statements.BCStatement;

/** Changes the colour that a lapis or daizuli pipe uses. The pipe does it when it sees the action. */
public class ActionPipeColor extends BCStatement implements IActionInternal {
    public final DyeColor colour;

    public ActionPipeColor(DyeColor colour) {
        super("buildcraft:pipe.color." + colour.getName(), "buildcraft.pipe." + colour.getName());
        this.colour = colour;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.action.pipe.item.color",
            Component.translatable("color.minecraft." + colour.getName()));
    }

    @Override
    public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
        // The pipe listens for this
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("pipe_colour_" + colour.getName());
    }

    @Override
    public IStatement[] getPossible() {
        return BCTransportStatements.ACTION_PIPE_COLOUR;
    }
}
