/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/** The block of a BuildCraft fluid in the world. It can burn, and slow down entities like cobwebs ("sticky"), both
 * depending on the config. */
public class BCFluidBlock extends LiquidBlock {
    private static final Vec3 STICKY_SLOWDOWN = new Vec3(0.25, 0.05, 0.25);

    private final BooleanSupplier flammable;
    private final BooleanSupplier sticky;

    public BCFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties, BooleanSupplier flammable,
        BooleanSupplier sticky) {
        super(fluid, properties);
        this.flammable = flammable;
        this.sticky = sticky;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (sticky.getAsBoolean()) {
            entity.makeStuckInBlock(state, STICKY_SLOWDOWN);
        }
    }

    @Override
    public MutableComponent getName() {
        return getFluid().getFluidType().getDescription().copy();
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean();
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean() ? 200 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean() ? 200 : 0;
    }
}
