/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.lib.fluid.BCBucketItem;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidBlock;
import buildcraft.lib.fluid.BCFluidType;
import buildcraft.lib.registry.BCRegistries;

import buildcraft.BuildCraft;

/** Oil, and everything that refining it makes. Each comes in 3 heat levels (cool, hot and searing), which the
 * refinery uses. Fluids hot enough to boil are gases. */
public final class BCEnergyFluids {
    /** Every oil fluid, in registration order. */
    public static final List<BCFluid> ALL = new ArrayList<>();

    // Tabular form of all the fluid values, from 1.12
    // @formatter:off
    //                                 name, density, viscosity, boil, spread, tex_light,  tex_dark, sticky, burns
    public static final List<BCFluid> CRUDE_OIL = defineFluids(
        "oil",                                     900,      2000,    3,      6, 0x50_50_50, 0x05_05_05,  true,  true);
    public static final List<BCFluid> OIL_RESIDUE = defineFluids(
        "oil_residue",                            1200,      4000,    3,      4, 0x10_0F_10, 0x42_10_42,  true, false);
    public static final List<BCFluid> OIL_HEAVY = defineFluids(
        "oil_heavy",                               850,      1800,    3,      6, 0xA0_8F_1F, 0x42_35_20,  true,  true);
    public static final List<BCFluid> OIL_DENSE = defineFluids(
        "oil_dense",                               950,      1600,    3,      5, 0x87_6E_77, 0x42_24_24,  true,  true);
    public static final List<BCFluid> OIL_DISTILLED = defineFluids(
        "oil_distilled",                           750,      1400,    2,      8, 0xE4_AF_78, 0xB4_7F_00, false,  true);
    public static final List<BCFluid> FUEL_DENSE = defineFluids(
        "fuel_dense",                              600,       800,    2,      7, 0xFF_AF_3F, 0xE0_7F_00, false,  true);
    public static final List<BCFluid> FUEL_MIXED_HEAVY = defineFluids(
        "fuel_mixed_heavy",                        700,      1000,    2,      7, 0xF2_A7_00, 0xC4_87_00, false,  true);
    public static final List<BCFluid> FUEL_LIGHT = defineFluids(
        "fuel_light",                              400,       600,    1,      8, 0xFF_FF_30, 0xE4_CF_00, false,  true);
    public static final List<BCFluid> FUEL_MIXED_LIGHT = defineFluids(
        "fuel_mixed_light",                        650,       900,    1,      9, 0xF6_D7_00, 0xC4_B7_00, false,  true);
    public static final List<BCFluid> FUEL_GASEOUS = defineFluids(
        "fuel_gaseous",                            300,       500,    0,     10, 0xFA_F6_30, 0xE0_D9_00, false,  true);
    // @formatter:on

    private BCEnergyFluids() {}

    /** Defines the 3 heat levels of a fluid. */
    private static List<BCFluid> defineFluids(String name, int density, int viscosity, int boilPoint, int spread,
        int texLight, int texDark, boolean sticky, boolean flammable) {
        List<BCFluid> list = new ArrayList<>();
        for (int heat = 0; heat < 3; heat++) {
            list.add(defineFluid(name, heat, density, viscosity, boilPoint, spread, texDark, sticky, flammable));
        }
        return Collections.unmodifiableList(list);
    }

    private static BCFluid defineFluid(String name, int heat, int density, int baseViscosity, int boilPoint,
        int baseSpread, int texDark, boolean sticky, boolean flammable) {
        String fullName = name + (heat == 0 ? "" : "_heat_" + heat);
        int viscosity = baseViscosity * (4 - heat) / 4;
        // Fluids hotter than their boiling point are gases, which go up rather than down
        int adjustedDensity = density * (heat >= boilPoint ? -1 : 1);
        // How far the fluid flows in the world. Higher heat values travel a little further
        int spread = baseSpread + (baseSpread > 6 ? heat : heat / 2);

        String texture = "block/fluid/" + name + "_heat_" + heat;
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, texture + "_still");
        ResourceLocation flow = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, texture + "_flow");

        RegistryObject<BCFluidType> type = BCRegistries.FLUID_TYPES.register(fullName, () -> new BCFluidType(
            FluidType.Properties.create()
                .descriptionId("fluid." + BuildCraft.MODID + "." + name)
                .density(adjustedDensity)
                .viscosity(viscosity)
                .temperature(300 + 20 * heat)
                .canConvertToSource(false)
                .canExtinguish(false)
                .supportsBoating(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY),
            "fluid." + BuildCraft.MODID + "." + name, heat, true, still, flow
        ));

        // The fluids, block and bucket all refer to each other, so they are looked up once everything is registered
        BCFluid[] self = new BCFluid[1];
        Supplier<ForgeFlowingFluid.Properties> properties = Suppliers.memoize(
            () -> new ForgeFlowingFluid.Properties(type, self[0].source, self[0].flowing)
                .bucket(self[0].bucket)
                .block(self[0].block)
                .slopeFindDistance(Math.max(1, Math.min(4, spread / 2)))
                .levelDecreasePerBlock(spread >= 7 ? 1 : spread >= 4 ? 2 : 3)
                .tickRate(Math.max(5, viscosity / 200))
        );

        RegistryObject<ForgeFlowingFluid.Source> source = BCRegistries.FLUIDS.register(
            fullName, () -> new ForgeFlowingFluid.Source(properties.get())
        );
        RegistryObject<ForgeFlowingFluid.Flowing> flowing = BCRegistries.FLUIDS.register(
            fullName + "_flowing", () -> new ForgeFlowingFluid.Flowing(properties.get())
        );
        RegistryObject<BCFluidBlock> block = BCRegistries.BLOCKS.register(
            fullName, () -> new BCFluidBlock(
                source, BlockBehaviour.Properties.of().mapColor(getMapColor(texDark)).replaceable().noCollission()
                    .strength(100f).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY),
                () -> flammable && BCEnergyConfig.oilCanBurn(), () -> sticky && BCEnergyConfig.oilIsSticky()
            )
        );
        RegistryObject<BCBucketItem> bucket = BCRegistries.ITEMS.register(
            fullName + "_bucket", () -> new BCBucketItem(
                source, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
        );

        BCFluid fluid = new BCFluid(fullName, type, source, flowing, block, bucket);
        self[0] = fluid;
        ALL.add(fluid);
        return fluid;
    }

    /** @return The map colour closest to the given RGB colour. */
    private static MapColor getMapColor(int color) {
        MapColor best = MapColor.COLOR_BLACK;
        int bestDifference = Integer.MAX_VALUE;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int id = 1; id < 64; id++) {
            MapColor mapColor = MapColor.byId(id);
            if (mapColor == MapColor.NONE || mapColor.col == 0) {
                continue;
            }
            int dr = ((mapColor.col >> 16) & 0xFF) - r;
            int dg = ((mapColor.col >> 8) & 0xFF) - g;
            int db = (mapColor.col & 0xFF) - b;
            int difference = dr * dr + dg * dg + db * db;
            if (difference < bestDifference) {
                bestDifference = difference;
                best = mapColor;
            }
        }
        return best;
    }

    /** Loads this class, so that every fluid is added to the registers. */
    static void init() {}
}
