/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import buildcraft.api.facades.IFacade;

import buildcraft.BuildCraft;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.client.gui.GuiDiamondPipe;
import buildcraft.transport.client.gui.GuiDiamondWoodPipe;
import buildcraft.transport.client.gui.GuiEmzuliPipe;
import buildcraft.transport.client.model.PipeBakedModel;
import buildcraft.transport.client.model.PlugModels;
import buildcraft.transport.client.render.RenderPipeHolder;
import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.plug.PluggablePowerAdaptor;
import buildcraft.transport.tile.TilePipeHolder;

@Mod.EventBusSubscriber(modid = BuildCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BCTransportClient {
    private BCTransportClient() {}

    static {
        ResourceLocation plug = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/pipe/plug");
        ResourceLocation adaptor = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/pipe/power_adapter");
        PlugModels.register(PluggableBlocker.Key.class, List.of(
            new PlugModels.Box(new float[] { 2, 4, 4 }, new float[] { 4.01f, 12, 12 }, plug)
        ));
        PlugModels.register(PluggablePowerAdaptor.Key.class, List.of(
            new PlugModels.Box(new float[] { 2, 4, 4 }, new float[] { 4.01f, 12, 12 }, adaptor),
            new PlugModels.Box(new float[] { 0, 3, 3 }, new float[] { 2, 13, 13 }, adaptor)
        ));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BCTransportMenus.DIAMOND_PIPE.get(), GuiDiamondPipe::new);
            MenuScreens.register(BCTransportMenus.DIAMOND_WOOD_PIPE.get(), GuiDiamondWoodPipe::new);
            MenuScreens.register(BCTransportMenus.EMZULI_PIPE.get(), GuiEmzuliPipe::new);
        });
    }

    /** Tints the quads of plugs that look like blocks (like a facade of grass) like the block would be. */
    @SubscribeEvent
    public static void registerBlockColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex < PlugModels.TINT_OFFSET || level == null || pos == null) {
                return -1;
            }
            int index = tintIndex - PlugModels.TINT_OFFSET;
            Direction side = Direction.from3DDataValue(index / 100);
            if (level.getBlockEntity(pos) instanceof TilePipeHolder tile
                && tile.getPluggable(side) instanceof IFacade facade) {
                BlockState facadeState = facade.getPhasedStates()[0].getState().getBlockState();
                return Minecraft.getInstance().getBlockColors().getColor(facadeState, level, pos, index % 100);
            }
            return -1;
        }, BCTransportBlocks.PIPE_HOLDER.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCTransportBlocks.PIPE_HOLDER_TILE.get(), RenderPipeHolder::new);
    }

    /** Replaces the placeholder model of the pipe holder (from its block state file) with the dynamic pipe model. */
    @SubscribeEvent
    public static void replacePipeModel(ModelEvent.ModifyBakingResult event) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "block/pipe_holder");
        BCTransportBlocks.PIPE_HOLDER.get().getStateDefinition().getPossibleStates().forEach(state -> {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            event.getModels().put(location, new PipeBakedModel(modelId));
        });
    }
}
