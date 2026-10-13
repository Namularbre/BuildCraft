/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

import buildcraft.api.statements.StatementManager;

/** The statements of pipes. (The pipe signal and pipe colour ones need pipe wires and lapis pipes, which aren't
 * ported.) */
public final class BCTransportStatements {
    public static final TriggerPowerRequested TRIGGER_POWER_REQUESTED = new TriggerPowerRequested();
    public static final TriggerItemsTraversing TRIGGER_ITEMS_TRAVERSING = new TriggerItemsTraversing();
    public static final TriggerFluidsTraversing TRIGGER_FLUIDS_TRAVERSING = new TriggerFluidsTraversing();
    public static final ActionPipeDirection[] ACTION_PIPE_DIRECTION = new ActionPipeDirection[6];
    /** For each colour: "on", then "off". */
    public static final TriggerPipeSignal[] TRIGGER_PIPE_SIGNAL = new TriggerPipeSignal[2 * 16];
    public static final ActionPipeSignal[] ACTION_PIPE_SIGNAL = new ActionPipeSignal[16];

    static {
        for (DyeColor colour : DyeColor.values()) {
            TRIGGER_PIPE_SIGNAL[colour.ordinal() * 2] = new TriggerPipeSignal(true, colour);
            TRIGGER_PIPE_SIGNAL[colour.ordinal() * 2 + 1] = new TriggerPipeSignal(false, colour);
            ACTION_PIPE_SIGNAL[colour.ordinal()] = new ActionPipeSignal(colour);
        }
        for (Direction face : Direction.values()) {
            ACTION_PIPE_DIRECTION[face.ordinal()] = new ActionPipeDirection(face);
        }
    }

    private BCTransportStatements() {}

    public static void init() {
        StatementManager.registerTriggerProvider(TriggerProviderPipes.INSTANCE);
        StatementManager.registerActionProvider(ActionProviderPipes.INSTANCE);
        StatementManager.registerParameter(TriggerParameterSignal::readFromNbt, TriggerParameterSignal::readFromBuf);
        StatementManager.registerParameter(ActionParameterSignal::readFromNbt);
    }
}
