/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

import buildcraft.BuildCraft;

/** A fluid tank for GameTests, until BuildCraft's own tank is ported. Only exists in development runs. */
@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TestTank {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "test_tank");
    public static final int CAPACITY = 16_000;

    public static Block block;
    public static BlockEntityType<Tile> tileType;

    private TestTank() {}

    @SubscribeEvent
    @SuppressWarnings("ConstantConditions")
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            block = new TankBlock(BlockBehaviour.Properties.of());
            helper.register(ID, block);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            tileType = BlockEntityType.Builder.of(Tile::new, block).build(null);
            helper.register(ID, tileType);
        });
    }

    private static final class TankBlock extends BaseEntityBlock {
        TankBlock(Properties properties) {
            super(properties);
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Tile(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }
    }

    public static final class Tile extends BlockEntity {
        public final FluidTank tank = new FluidTank(CAPACITY);
        private final LazyOptional<FluidTank> tankCap = LazyOptional.of(() -> tank);

        public Tile(BlockPos pos, BlockState state) {
            super(tileType, pos, state);
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                return tankCap.cast();
            }
            return super.getCapability(cap, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            tankCap.invalidate();
        }
    }
}
