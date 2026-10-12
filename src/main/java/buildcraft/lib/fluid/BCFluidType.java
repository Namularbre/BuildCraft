/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

/** A BuildCraft fluid type. Fluids that can be heated show their heat level after their name, like "Oil (Hot)". */
public class BCFluidType extends FluidType {
    /** The translation key of the fluid's name, without its heat. */
    public final String baseNameKey;
    public final int heat;
    public final boolean heatable;
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;

    public BCFluidType(Properties properties, String baseNameKey, int heat, boolean heatable,
        ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties);
        this.baseNameKey = baseNameKey;
        this.heat = heat;
        this.heatable = heatable;
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    /** @return The texture of the still fluid (also available on the server, unlike the client extensions). */
    public ResourceLocation getStillTextureLocation() {
        return stillTexture;
    }

    @Override
    public Component getDescription() {
        Component name = Component.translatable(baseNameKey);
        if (!heatable) {
            return name;
        }
        return Component.translatable("buildcraft.fluid.heat_" + heat, name);
    }

    @Override
    public Component getDescription(FluidStack stack) {
        return getDescription();
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }
        });
    }

    /** @return The heat of the given fluid, or 0 if it isn't a heatable BuildCraft fluid. */
    public static int getHeat(Fluid fluid) {
        return fluid.getFluidType() instanceof BCFluidType type ? type.heat : 0;
    }
}
