/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEventHandler;
import buildcraft.api.transport.pipe.PipeEventStatement;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.statement.BCSiliconStatements;

/** Gives the gates of the pipe triggers about the light in front of it. */
public class PluggableLightSensor extends PipePluggable {
    private static final AABB[] BOXES = PluggableGate.boxes();

    public PluggableLightSensor(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
    }

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.PLUG_LIGHT_SENSOR.get());
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new KeyPlugLightSensor(side);
    }

    @PipeEventHandler
    public void addInternalTriggers(PipeEventStatement.AddTriggerInternalSided event) {
        if (event.side == this.side) {
            event.triggers.add(BCSiliconStatements.TRIGGER_LIGHT_LOW);
            event.triggers.add(BCSiliconStatements.TRIGGER_LIGHT_HIGH);
        }
    }

    public static final class KeyPlugLightSensor extends PluggableModelKey {
        public KeyPlugLightSensor(Direction side) {
            super(side);
        }
    }
}
