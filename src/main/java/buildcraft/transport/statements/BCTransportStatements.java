/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import net.minecraft.core.Direction;

import buildcraft.api.statements.StatementManager;

/** The statements of pipes. (The pipe signal and pipe colour ones need pipe wires and lapis pipes, which aren't
 * ported.) */
public final class BCTransportStatements {
    public static final TriggerPowerRequested TRIGGER_POWER_REQUESTED = new TriggerPowerRequested();
    public static final TriggerItemsTraversing TRIGGER_ITEMS_TRAVERSING = new TriggerItemsTraversing();
    public static final TriggerFluidsTraversing TRIGGER_FLUIDS_TRAVERSING = new TriggerFluidsTraversing();
    public static final ActionPipeDirection[] ACTION_PIPE_DIRECTION = new ActionPipeDirection[6];

    static {
        for (Direction face : Direction.values()) {
            ACTION_PIPE_DIRECTION[face.ordinal()] = new ActionPipeDirection(face);
        }
    }

    private BCTransportStatements() {}

    public static void init() {
        StatementManager.registerTriggerProvider(TriggerProviderPipes.INSTANCE);
        StatementManager.registerActionProvider(ActionProviderPipes.INSTANCE);
    }
}
