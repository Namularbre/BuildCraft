/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import buildcraft.BuildCraft;
import buildcraft.api.enums.EnumPowerStage;
import buildcraft.lib.engine.BlockEngineBase;
import buildcraft.lib.fluid.BCFluid;

import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockTank;
import buildcraft.transport.BCTransportBlocks;

public class BCBlockStateProvider extends BlockStateProvider {
    public BCBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, BuildCraft.MODID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        engine(BCCoreBlocks.ENGINE_REDSTONE.get(), "redstone");
        engine(BCEnergyBlocks.ENGINE_STONE.get(), "stone");
        engine(BCEnergyBlocks.ENGINE_IRON.get(), "iron");

        if (BCCoreBlocks.POWER_TESTER != null) {
            simpleBlock(BCCoreBlocks.POWER_TESTER.get());
        }

        // Fluids are drawn by the fluid renderer: the model is only used for the breaking particles
        for (BCFluid fluid : BCEnergyFluids.ALL) {
            ResourceLocation still = fluid.type.get().getStillTextureLocation();
            simpleBlock(fluid.block.get(), models().getBuilder("block/fluid/" + fluid.name).texture("particle", still));
        }

        getVariantBuilder(BCFactoryBlocks.TANK.get()).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(models().getExistingFile(modLoc(
                state.getValue(BlockTank.JOINED_BELOW) ? "block/tank_joined_below" : "block/tank"
            )))
            .build());

        // A placeholder: the real model is generated from the pipe at runtime
        simpleBlock(BCTransportBlocks.PIPE_HOLDER.get(), models().getExistingFile(modLoc("block/pipe_holder")));
    }

    /** One model per heat stage, rotated so that the engine's trunk points in the direction it faces. */
    private void engine(BlockEngineBase block, String name) {
        getVariantBuilder(block).forAllStates(state -> {
            EnumPowerStage stage = state.getValue(BlockEngineBase.STAGE);
            ModelFile model = models().withExistingParent("block/engine/" + name + "_" + stage.getSerializedName(),
                modLoc("block/engine_base"))
                .texture("back", modLoc("block/engine/" + name + "_back"))
                .texture("side", modLoc("block/engine/" + name + "_side"))
                .texture("trunk", modLoc("block/engine/trunk_" + stage.getSerializedName()));

            // The model faces up
            Direction facing = state.getValue(BlockEngineBase.FACING);
            int x = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;
            int y = switch (facing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
    }
}
