/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

import buildcraft.lib.fluid.BCFluid;

import buildcraft.BuildCraft;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.factory.BCFactoryItems;
import buildcraft.silicon.EnumRedstoneChipset;
import buildcraft.transport.BCTransportItems;

public class BCItemModelProvider extends ItemModelProvider {
    public BCItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, BuildCraft.MODID, files);
    }

    @Override
    protected void registerModels() {
        withExistingParent("wrench", mcLoc("item/handheld")).texture("layer0", modLoc("item/wrench"));
        basicItem(BCCoreItems.GEAR_WOOD.get());
        basicItem(BCCoreItems.GEAR_STONE.get());
        basicItem(BCCoreItems.GEAR_IRON.get());
        basicItem(BCCoreItems.GEAR_GOLD.get());
        basicItem(BCCoreItems.GEAR_DIAMOND.get());
        basicItem(BCTransportItems.PIPE_SEALANT.get());

        engine(BCCoreBlocks.ENGINE_REDSTONE.getId(), "redstone");
        engine(BCEnergyBlocks.ENGINE_STONE.getId(), "stone");
        engine(BCEnergyBlocks.ENGINE_IRON.getId(), "iron");
        withExistingParent("tank", modLoc("block/tank"));
        withExistingParent("pump", modLoc("block/pump"));
        withExistingParent("mining_well", modLoc("block/mining_well"));
        withExistingParent("chute", modLoc("block/chute"));
        withExistingParent("water_gel_spawn", mcLoc("item/generated")).texture("layer0", modLoc("item/water_gel"));
        basicItem(BCFactoryItems.GEL.get());

        withExistingParent("laser", modLoc("block/laser"));
        withExistingParent("quarry", modLoc("block/quarry/quarry"));
        withExistingParent("marker_volume", mcLoc("item/generated")).texture("layer0", modLoc("item/marker_volume"));
        withExistingParent("assembly_table", modLoc("block/assembly_table"));
        for (EnumRedstoneChipset chipset : EnumRedstoneChipset.values()) {
            withExistingParent(chipset.getItemName(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/chipset/" + chipset.getTextureName()));
        }
        withExistingParent("autoworkbench_item", modLoc("block/autoworkbench_item"));
        withExistingParent("flood_gate", modLoc("block/flood_gate_d_n_s_w_e"));

        if (BCCoreBlocks.POWER_TESTER != null) {
            withExistingParent(BCCoreBlocks.POWER_TESTER.getId().getPath(), modLoc("block/power_tester"));
        }

        // Forge's bucket model, which draws the fluid inside the bucket
        for (BCFluid fluid : BCEnergyFluids.ALL) {
            withExistingParent(fluid.bucket.getId().getPath(), ResourceLocation.fromNamespaceAndPath(
                "forge", "item/bucket_drip"
            )).customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(fluid.get())
                .flipGas(true);
        }

        BCTransportItems.PIPES.forEach((definition, item) -> {
            int texture = definition.itemModelCenter.getTexture(0);
            String path = ResourceLocation.parse(definition.textures[texture]).getPath();
            withExistingParent(item.getId().getPath(), modLoc("item/pipe_base")).texture("pipe", modLoc(path));
        });
    }

    /** The whole engine at rest, in its coolest stage. */
    private void engine(ResourceLocation id, String name) {
        withExistingParent(id.getPath(), modLoc("item/engine_base"))
            .texture("back", modLoc("block/engine/" + name + "_back"))
            .texture("side", modLoc("block/engine/" + name + "_side"))
            .texture("trunk", modLoc("block/engine/trunk_blue"));
    }
}
