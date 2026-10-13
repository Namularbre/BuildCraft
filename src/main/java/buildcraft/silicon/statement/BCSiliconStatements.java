/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.statement;

/** The statements of the silicon pluggables. */
public final class BCSiliconStatements {
    public static final TriggerLightSensor TRIGGER_LIGHT_LOW = new TriggerLightSensor(false);
    public static final TriggerLightSensor TRIGGER_LIGHT_HIGH = new TriggerLightSensor(true);
    public static final TriggerLightSensor[] TRIGGER_LIGHT = { TRIGGER_LIGHT_LOW, TRIGGER_LIGHT_HIGH };

    public static final ActionPowerPulsar ACTION_PULSAR_CONSTANT = new ActionPowerPulsar(true);
    public static final ActionPowerPulsar ACTION_PULSAR_SINGLE = new ActionPowerPulsar(false);
    public static final ActionPowerPulsar[] ACTION_PULSAR = { ACTION_PULSAR_CONSTANT, ACTION_PULSAR_SINGLE };

    private BCSiliconStatements() {}

    /** Loads this class, so that every statement is registered. */
    public static void init() {}
}
