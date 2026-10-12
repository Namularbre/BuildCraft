/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import net.minecraft.world.level.material.FlowingFluid;

import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.RegistryObject;

/** Everything registered for a single BuildCraft fluid: its type, the source and flowing fluids, the block it forms
 * in the world, and its bucket. */
public final class BCFluid {
    /** The registry name, like "oil" or "oil_heat_1". */
    public final String name;
    public final RegistryObject<BCFluidType> type;
    public final RegistryObject<ForgeFlowingFluid.Source> source;
    public final RegistryObject<ForgeFlowingFluid.Flowing> flowing;
    public final RegistryObject<BCFluidBlock> block;
    public final RegistryObject<BCBucketItem> bucket;

    public BCFluid(String name, RegistryObject<BCFluidType> type, RegistryObject<ForgeFlowingFluid.Source> source,
        RegistryObject<ForgeFlowingFluid.Flowing> flowing, RegistryObject<BCFluidBlock> block,
        RegistryObject<BCBucketItem> bucket) {
        this.name = name;
        this.type = type;
        this.source = source;
        this.flowing = flowing;
        this.block = block;
        this.bucket = bucket;
    }

    /** @return The source fluid, which is the one that goes in tanks and pipes. */
    public FlowingFluid get() {
        return source.get();
    }

    public int getHeat() {
        return type.get().heat;
    }
}
