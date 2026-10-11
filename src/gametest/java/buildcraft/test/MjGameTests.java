/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjPassiveProvider;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.mj.MjRfConversion;
import buildcraft.api.mj.MjToRfAutoConvertor;

import buildcraft.BuildCraft;

@GameTestHolder(BuildCraft.MODID)
@PrefixGameTestTemplate(false)
public class MjGameTests {
    private static final String EMPTY = "gametest/empty3x3x3";

    @GameTest(template = EMPTY)
    public static void batteryAddAndExtract(GameTestHelper helper) {
        MjBattery battery = new MjBattery(10 * MjAPI.MJ);
        assertEquals(helper, 0, battery.addPower(4 * MjAPI.MJ, false));
        assertEquals(helper, 4 * MjAPI.MJ, battery.getStored());

        // Not enough for the minimum
        assertEquals(helper, 0, battery.extractPower(5 * MjAPI.MJ, 6 * MjAPI.MJ));
        assertEquals(helper, 3 * MjAPI.MJ, battery.extractPower(MjAPI.MJ, 3 * MjAPI.MJ));
        assertEquals(helper, MjAPI.MJ, battery.getStored());

        battery.addPower(4 * MjAPI.MJ, true);
        assertEquals(helper, MjAPI.MJ, battery.getStored());

        CompoundTag nbt = battery.serializeNBT();
        MjBattery read = new MjBattery(10 * MjAPI.MJ);
        read.deserializeNBT(nbt);
        assertEquals(helper, MjAPI.MJ, read.getStored());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void rfConversionConfig(GameTestHelper helper) {
        helper.assertTrue(MjAPI.isRfAutoConversionEnabled(), "RF auto conversion should be on by default");
        assertEquals(helper, MjRfConversion.DEFAULT_MJ_PER_RF, MjAPI.getRfConversion().mjPerRf);
        helper.assertTrue(MjRfConversion.createParsed(1).usingDefaultValue, "1 MJ/RF is out of range");
        assertEquals(helper, MjAPI.MJ / 20, MjRfConversion.createParsed(0.05).mjPerRf);
        helper.succeed();
    }

    /** Forge Energy pushed into an MJ machine. */
    @GameTest(template = EMPTY)
    public static void rfIntoMjReceiver(GameTestHelper helper) {
        TestMachine machine = new TestMachine(10 * MjAPI.MJ);
        MjCapabilityHelper caps = new MjCapabilityHelper(machine);
        IEnergyStorage rf = caps.getCapability(ForgeCapabilities.ENERGY, null)
            .orElseThrow(IllegalStateException::new);

        // 10 RF per MJ by default
        assertEquals(helper, 25, rf.receiveEnergy(25, false));
        assertEquals(helper, 25 * MjAPI.MJ / 10, machine.battery.getStored());
        assertEquals(helper, 25, rf.getEnergyStored());
        assertEquals(helper, 100, rf.getMaxEnergyStored());

        // Only 2 RF of space left: accept those, rather than refusing everything
        machine.battery.addPower(10 * MjAPI.MJ - machine.battery.getStored() - MjAPI.MJ / 5, false);
        assertEquals(helper, 2, rf.receiveEnergy(25, true));
        assertEquals(helper, 2, rf.receiveEnergy(25, false));
        assertEquals(helper, 0, rf.receiveEnergy(25, false));

        caps.invalidate();
        helper.assertFalse(caps.getCapability(MjAPI.CAP_RECEIVER, null).isPresent(), "Caps should be invalidated");
        helper.succeed();
    }

    /** Forge Energy pulled out of an MJ provider. */
    @GameTest(template = EMPTY)
    public static void rfOutOfMjProvider(GameTestHelper helper) {
        TestMachine machine = new TestMachine(10 * MjAPI.MJ);
        machine.battery.addPower(MjAPI.MJ + MjAPI.MJ / 20, false);
        IEnergyStorage rf = new MjCapabilityHelper(machine).getCapability(ForgeCapabilities.ENERGY, null)
            .orElseThrow(IllegalStateException::new);

        helper.assertTrue(rf.canExtract(), "Provider should allow extraction");
        assertEquals(helper, 10, rf.extractEnergy(100, false));
        // The half RF left over can't be extracted
        assertEquals(helper, 0, rf.extractEnergy(100, false));
        assertEquals(helper, MjAPI.MJ / 20, machine.battery.getStored());
        helper.succeed();
    }

    /** An MJ machine pushing into a Forge Energy storage. */
    @GameTest(template = EMPTY)
    public static void mjIntoRfStorage(GameTestHelper helper) {
        EnergyStorage storage = new EnergyStorage(100);
        IMjReceiver receiver = MjToRfAutoConvertor.createReceiver(storage);
        helper.assertTrue(receiver != null, "An FE storage that can receive should become an MJ receiver");

        // 1.05 MJ = 10.5 RF: 10 RF are stored, the rest comes back as excess
        long excess = receiver.receivePower(MjAPI.MJ + MjAPI.MJ / 20, false);
        assertEquals(helper, 10, storage.getEnergyStored());
        assertEquals(helper, MjAPI.MJ / 20, excess);
        helper.succeed();
    }

    private static void assertEquals(GameTestHelper helper, long expected, long actual) {
        helper.assertTrue(expected == actual, "Expected " + expected + " but got " + actual);
    }

    private static final class TestMachine implements IMjReceiver, IMjReadable, IMjPassiveProvider {
        final MjBattery battery;

        TestMachine(long capacity) {
            battery = new MjBattery(capacity);
        }

        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return battery.getCapacity() - battery.getStored();
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            long accepted = Math.min(microJoules, getPowerRequested());
            battery.addPower(accepted, simulate);
            return microJoules - accepted;
        }

        @Override
        public long extractPower(long min, long max, boolean simulate) {
            if (simulate) {
                long stored = battery.getStored();
                return stored < min ? 0 : Math.min(stored, max);
            }
            return battery.extractPower(min, max);
        }

        @Override
        public long getStored() {
            return battery.getStored();
        }

        @Override
        public long getCapacity() {
            return battery.getCapacity();
        }
    }
}
