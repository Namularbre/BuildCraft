/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;

/** A bucket named after its fluid ("Bucket of Oil (Hot)"), so that every fluid doesn't need its own translation. */
public class BCBucketItem extends BucketItem {
    public BCBucketItem(Supplier<? extends Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    /** Forge only gives this to {@link BucketItem} itself, not to its subclasses: without it, tanks (and anything
     * else using the fluid capability) can't empty or fill the bucket. */
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidBucketWrapper(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcraft.fluid_bucket", getFluid().getFluidType().getDescription());
    }
}
