/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import net.minecraftforge.common.ForgeConfigSpec;

import buildcraft.api.mj.IMjToRfStatus;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjRfConversion;

/** BuildCraft's common (server-wide) config. */
public final class BCLibConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue RF_AUTO_CONVERT;
    public static final ForgeConfigSpec.DoubleValue MJ_PER_RF;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("power");
        RF_AUTO_CONVERT = builder
            .comment("If true, BuildCraft machines and other mods' Forge Energy (RF/FE) blocks will automatically "
                + "convert power between MJ and FE.")
            .define("rfAutoConvert", true);
        MJ_PER_RF = builder
            .comment("How many MJ a single FE is worth when converting.")
            .defineInRange(
                "mjPerRf", MjRfConversion.DEFAULT_MJ_PER_RF / (double) MjAPI.MJ,
                MjRfConversion.MIN_MJ_PER_RF / (double) MjAPI.MJ, MjRfConversion.MAX_MJ_PER_RF / (double) MjAPI.MJ
            );
        builder.pop();

        SPEC = builder.build();
    }

    private BCLibConfig() {}

    /** Reads the RF conversion values from the config, falling back to the defaults before it's loaded. */
    public static final class MjToRfStatus implements IMjToRfStatus {
        private static final MjRfConversion DEFAULT = MjRfConversion.createDefault();

        private double cachedMjPerRf = Double.NaN;
        private MjRfConversion cachedConversion = DEFAULT;

        @Override
        public MjRfConversion getConversion() {
            if (!SPEC.isLoaded()) {
                return DEFAULT;
            }
            double mjPerRf = MJ_PER_RF.get();
            if (mjPerRf != cachedMjPerRf) {
                cachedMjPerRf = mjPerRf;
                cachedConversion = MjRfConversion.createParsed(mjPerRf);
            }
            return cachedConversion;
        }

        @Override
        public boolean isAutoconvertEnabled() {
            return SPEC.isLoaded() && RF_AUTO_CONVERT.get();
        }
    }
}
