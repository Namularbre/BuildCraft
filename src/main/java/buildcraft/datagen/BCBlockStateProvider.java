/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import buildcraft.api.enums.EnumPowerStage;

import buildcraft.lib.engine.BlockEngineBase;
import buildcraft.lib.fluid.BCFluid;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockHeatExchange.EnumExchangePart;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.block.BlockLaser;
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

        simpleBlock(BCFactoryBlocks.PUMP.get(), models().cube(
            "block/pump", modLoc("block/pump/bottom"), modLoc("block/pump/top"), modLoc("block/pump/side"),
            modLoc("block/pump/side"), modLoc("block/pump/side"), modLoc("block/pump/side")
        ).texture("particle", modLoc("block/pump/side")));
        // The front (with the LEDs) faces north in the model, and is turned to face where the block faces
        horizontalBlock(BCFactoryBlocks.MINING_WELL.get(), models().cube(
            "block/mining_well", modLoc("block/mining_well/bottom"), modLoc("block/mining_well/top"),
            modLoc("block/mining_well/front"), modLoc("block/mining_well/back"), modLoc("block/mining_well/side"),
            modLoc("block/mining_well/side")
        ).texture("particle", modLoc("block/mining_well/side")));
        getVariantBuilder(BCFactoryBlocks.DISTILLER.get()).forAllStates(state -> ConfiguredModel.builder()
            .modelFile(models().getExistingFile(modLoc("block/distiller")))
            .rotationY(BlockDistiller.modelRotation(state.getValue(BlockDistiller.FACING)))
            .build());
        heatExchange();
        chute();
        laser();
        simpleBlock(BCSiliconBlocks.ASSEMBLY_TABLE.get(), models().getExistingFile(modLoc("block/assembly_table")));
        getVariantBuilder(BCFactoryBlocks.WATER_GEL.get()).forAllStates(state -> {
            String stage = state.getValue(BlockWaterGel.STAGE).getSerializedName();
            return ConfiguredModel.builder()
                .modelFile(models().cubeAll("block/water_gel/" + stage, modLoc("block/gel/" + stage))).build();
        });
        // Like in 1.12, the top texture is used for the bottom too
        simpleBlock(BCFactoryBlocks.AUTO_WORKBENCH_ITEMS.get(), models().cube(
            "block/autoworkbench_item", modLoc("block/auto_workbench_item/top"),
            modLoc("block/auto_workbench_item/top"), modLoc("block/auto_workbench_item/side"),
            modLoc("block/auto_workbench_item/side"), modLoc("block/auto_workbench_item/side"),
            modLoc("block/auto_workbench_item/side")
        ).texture("particle", modLoc("block/auto_workbench_item/side")));
        floodGate();
        // Drawn by the machine above it: the model is only used for particles
        simpleBlock(BCFactoryBlocks.TUBE.get(), models().getBuilder("block/tube")
            .texture("particle", modLoc("block/pump/tube")));

        // A placeholder: the real model is generated from the pipe at runtime
        simpleBlock(BCTransportBlocks.PIPE_HOLDER.get(), models().getExistingFile(modLoc("block/pipe_holder")));
    }

    /** A cube showing which sides are open, so one model for each combination of open sides. */
    private void floodGate() {
        ResourceLocation open = modLoc("block/flood_gate/open");
        ResourceLocation closed = modLoc("block/flood_gate/closed");
        getVariantBuilder(BCFactoryBlocks.FLOOD_GATE.get()).forAllStates(state -> {
            StringBuilder name = new StringBuilder("block/flood_gate");
            Map<Direction, ResourceLocation> textures = new EnumMap<>(Direction.class);
            for (Direction side : BlockFloodGate.OPEN.keySet()) {
                boolean isOpen = state.getValue(BlockFloodGate.OPEN.get(side));
                textures.put(side, isOpen ? open : closed);
                name.append(isOpen ? "_" + side.getSerializedName().charAt(0) : "");
            }
            ModelFile model = models().cube(name.toString(), textures.get(Direction.DOWN),
                modLoc("block/flood_gate/top"), textures.get(Direction.NORTH), textures.get(Direction.SOUTH),
                textures.get(Direction.EAST), textures.get(Direction.WEST)
            ).texture("particle", modLoc("block/flood_gate/top"));
            return ConfiguredModel.builder().modelFile(model).build();
        });
    }

    /** The model points up, and is turned like in 1.12. */
    private void laser() {
        ModelFile model = models().getExistingFile(modLoc("block/laser"));
        getVariantBuilder(BCSiliconBlocks.LASER.get()).forAllStates(state -> {
            Direction facing = state.getValue(BlockLaser.FACING);
            int[] rotation = switch (facing) {
                case UP -> new int[] { 0, 0 };
                case DOWN -> new int[] { 180, 0 };
                case NORTH -> new int[] { 90, 0 };
                case EAST -> new int[] { 90, 90 };
                case SOUTH -> new int[] { 90, 180 };
                case WEST -> new int[] { 90, 270 };
            };
            return ConfiguredModel.builder().modelFile(model).rotationX(rotation[0]).rotationY(rotation[1]).build();
        });
    }

    /** The funnel turned towards the opening, and a small spout towards each inventory it sends items to (the
     * rotations of 1.12). */
    private void chute() {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(BCFactoryBlocks.CHUTE.get());
        ModelFile main = models().getExistingFile(modLoc("block/chute"));
        ModelFile spout = models().getExistingFile(modLoc("block/chute_connected"));
        for (Direction facing : Direction.values()) {
            int[] rotation = switch (facing) {
                case UP -> new int[] { 0, 0 };
                case DOWN -> new int[] { 180, 0 };
                case NORTH -> new int[] { 90, 0 };
                case EAST -> new int[] { 90, 90 };
                case SOUTH -> new int[] { 90, 180 };
                case WEST -> new int[] { 90, 270 };
            };
            builder.part().modelFile(main).rotationX(rotation[0]).rotationY(rotation[1]).addModel()
                .condition(BlockChute.FACING, facing);
        }
        for (Direction side : Direction.values()) {
            int[] rotation = switch (side) {
                case UP -> new int[] { 180, 0 };
                case DOWN -> new int[] { 0, 0 };
                case NORTH -> new int[] { 90, 180 };
                case EAST -> new int[] { 90, 270 };
                case SOUTH -> new int[] { 90, 0 };
                case WEST -> new int[] { 90, 90 };
            };
            builder.part().modelFile(spout).rotationX(rotation[0]).rotationY(rotation[1]).addModel()
                .condition(BlockChute.CONNECTED.get(side), true);
        }
    }

    /** Built from parts: the inner pipe always shows, and the rest depends on which part of a heat exchanger the
     * block is. The models face west, like in 1.12. */
    private void heatExchange() {
        MultiPartBlockStateBuilder builder = getMultipartBuilder(BCFactoryBlocks.HEAT_EXCHANGE.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int rotation = BlockHeatExchange.modelRotation(facing);
            heatExchangePart(builder, "inner", rotation).condition(BlockHeatExchange.FACING, facing);
            heatExchangePart(builder, "middle", rotation).condition(BlockHeatExchange.FACING, facing)
                .condition(BlockHeatExchange.PART, EnumExchangePart.MIDDLE);
            heatExchangePart(builder, "cap_left", rotation).condition(BlockHeatExchange.FACING, facing)
                .condition(BlockHeatExchange.PART, EnumExchangePart.MIDDLE)
                .condition(BlockHeatExchange.CONNECTED_LEFT, false);
            heatExchangePart(builder, "cap_right", rotation).condition(BlockHeatExchange.FACING, facing)
                .condition(BlockHeatExchange.PART, EnumExchangePart.MIDDLE)
                .condition(BlockHeatExchange.CONNECTED_RIGHT, false);
            heatExchangePart(builder, "start", rotation).condition(BlockHeatExchange.FACING, facing)
                .condition(BlockHeatExchange.PART, EnumExchangePart.START);
            heatExchangePart(builder, "end", rotation).condition(BlockHeatExchange.FACING, facing)
                .condition(BlockHeatExchange.PART, EnumExchangePart.END);
        }
    }

    private MultiPartBlockStateBuilder.PartBuilder heatExchangePart(MultiPartBlockStateBuilder builder, String part,
        int rotation) {
        return builder.part().modelFile(models().getExistingFile(modLoc("block/heat_exchange_" + part)))
            .rotationY(rotation).addModel();
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
