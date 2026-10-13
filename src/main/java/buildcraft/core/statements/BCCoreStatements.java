/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.tiles.IControllable.Mode;

/** Every basic trigger and action. */
public final class BCCoreStatements {
    public static final TriggerTrue TRIGGER_TRUE = new TriggerTrue();

    public static final TriggerMachine TRIGGER_MACHINE_ACTIVE = new TriggerMachine(true);
    public static final TriggerMachine TRIGGER_MACHINE_INACTIVE = new TriggerMachine(false);
    public static final TriggerMachine[] TRIGGER_MACHINE = { TRIGGER_MACHINE_ACTIVE, TRIGGER_MACHINE_INACTIVE };

    public static final TriggerRedstoneInput TRIGGER_REDSTONE_ACTIVE = new TriggerRedstoneInput(true);
    public static final TriggerRedstoneInput TRIGGER_REDSTONE_INACTIVE = new TriggerRedstoneInput(false);
    public static final TriggerRedstoneInput[] TRIGGER_REDSTONE =
        { TRIGGER_REDSTONE_ACTIVE, TRIGGER_REDSTONE_INACTIVE };

    public static final ActionRedstoneOutput ACTION_REDSTONE = new ActionRedstoneOutput();

    public static final ActionMachineControl ACTION_MACHINE_CONTROL_OFF = new ActionMachineControl(Mode.OFF);
    public static final ActionMachineControl ACTION_MACHINE_CONTROL_ON = new ActionMachineControl(Mode.ON);
    public static final ActionMachineControl ACTION_MACHINE_CONTROL_LOOP = new ActionMachineControl(Mode.LOOP);
    public static final ActionMachineControl[] ACTION_MACHINE_CONTROL =
        { ACTION_MACHINE_CONTROL_OFF, ACTION_MACHINE_CONTROL_ON, ACTION_MACHINE_CONTROL_LOOP };

    public static final TriggerPower TRIGGER_POWER_HIGH = new TriggerPower(true);
    public static final TriggerPower TRIGGER_POWER_LOW = new TriggerPower(false);
    public static final TriggerPower[] TRIGGER_POWER = { TRIGGER_POWER_LOW, TRIGGER_POWER_HIGH };

    public static final TriggerInventory TRIGGER_INVENTORY_EMPTY = new TriggerInventory(TriggerInventory.State.EMPTY);
    public static final TriggerInventory TRIGGER_INVENTORY_CONTAINS =
        new TriggerInventory(TriggerInventory.State.CONTAINS);
    public static final TriggerInventory TRIGGER_INVENTORY_SPACE = new TriggerInventory(TriggerInventory.State.SPACE);
    public static final TriggerInventory TRIGGER_INVENTORY_FULL = new TriggerInventory(TriggerInventory.State.FULL);

    public static final TriggerFluidContainer TRIGGER_FLUID_EMPTY =
        new TriggerFluidContainer(TriggerFluidContainer.State.EMPTY);
    public static final TriggerFluidContainer TRIGGER_FLUID_CONTAINS =
        new TriggerFluidContainer(TriggerFluidContainer.State.CONTAINS);
    public static final TriggerFluidContainer TRIGGER_FLUID_SPACE =
        new TriggerFluidContainer(TriggerFluidContainer.State.SPACE);
    public static final TriggerFluidContainer TRIGGER_FLUID_FULL =
        new TriggerFluidContainer(TriggerFluidContainer.State.FULL);

    public static final TriggerInventoryLevel TRIGGER_INVENTORY_BELOW_25 =
        new TriggerInventoryLevel(TriggerInventoryLevel.TriggerType.BELOW25);
    public static final TriggerInventoryLevel TRIGGER_INVENTORY_BELOW_50 =
        new TriggerInventoryLevel(TriggerInventoryLevel.TriggerType.BELOW50);
    public static final TriggerInventoryLevel TRIGGER_INVENTORY_BELOW_75 =
        new TriggerInventoryLevel(TriggerInventoryLevel.TriggerType.BELOW75);

    public static final TriggerFluidContainerLevel TRIGGER_FLUID_BELOW_25 =
        new TriggerFluidContainerLevel(TriggerFluidContainerLevel.TriggerType.BELOW25);
    public static final TriggerFluidContainerLevel TRIGGER_FLUID_BELOW_50 =
        new TriggerFluidContainerLevel(TriggerFluidContainerLevel.TriggerType.BELOW50);
    public static final TriggerFluidContainerLevel TRIGGER_FLUID_BELOW_75 =
        new TriggerFluidContainerLevel(TriggerFluidContainerLevel.TriggerType.BELOW75);

    public static final TriggerEnginePowerStage[] TRIGGER_POWER_STAGES = {
        new TriggerEnginePowerStage(EnumPowerStage.BLUE), new TriggerEnginePowerStage(EnumPowerStage.GREEN),
        new TriggerEnginePowerStage(EnumPowerStage.YELLOW), new TriggerEnginePowerStage(EnumPowerStage.RED),
        new TriggerEnginePowerStage(EnumPowerStage.OVERHEAT) };

    public static final BCStatement[] TRIGGER_INVENTORY_ALL = { TRIGGER_INVENTORY_EMPTY, TRIGGER_INVENTORY_SPACE,
        TRIGGER_INVENTORY_CONTAINS, TRIGGER_INVENTORY_FULL, TRIGGER_INVENTORY_BELOW_25, TRIGGER_INVENTORY_BELOW_50,
        TRIGGER_INVENTORY_BELOW_75 };
    public static final BCStatement[] TRIGGER_FLUID_ALL = { TRIGGER_FLUID_EMPTY, TRIGGER_FLUID_SPACE,
        TRIGGER_FLUID_CONTAINS, TRIGGER_FLUID_FULL, TRIGGER_FLUID_BELOW_25, TRIGGER_FLUID_BELOW_50,
        TRIGGER_FLUID_BELOW_75 };

    private BCCoreStatements() {}

    /** Registers the providers and parameters (the statements register themselves when this class is loaded). */
    public static void init() {
        StatementManager.registerParameter(StatementParamGateSideOnly::readFromNbt);
        StatementManager.registerTriggerProvider(CoreTriggerProvider.INSTANCE);
        StatementManager.registerActionProvider(CoreActionProvider.INSTANCE);
    }
}
