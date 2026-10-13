/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;

import buildcraft.api.facades.FacadeAPI;

import buildcraft.lib.registry.BCRegistries;

import buildcraft.BuildCraft;
import buildcraft.core.BCCore;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.silicon.statement.BCSiliconStatements;

/** The silicon module: lasers, and the tables they power. */
public final class BCSilicon {
    private BCSilicon() {}

    public static void init(FMLJavaModLoadingContext context) {
        BCSiliconBlocks.init();
        BCSiliconItems.init();
        BCSiliconMenus.init();
        BCSiliconPlugs.init();
        BCSiliconStatements.init();
        BCSiliconRecipeSerializers.init();
        FacadeAPI.registry = FacadeStateManager.INSTANCE;
        context.getModEventBus().addListener(BCSilicon::commonSetup);
    }

    /** Every facade (full and hollow) that can be made. */
    public static final RegistryObject<CreativeModeTab> FACADES_TAB = BCRegistries.CREATIVE_MODE_TABS.register(
        "facades", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + BuildCraft.MODID + ".facades"))
            .icon(() -> new ItemStack(BCSiliconItems.PLUG_FACADE.get()))
            .withTabsBefore(BCCore.CREATIVE_TAB.getId())
            .displayItems((params, output) -> {
                for (FacadeBlockStateInfo info : FacadeStateManager.validFacadeStates.values()) {
                    if (info.isVisible) {
                        ItemPluggableFacade item = BCSiliconItems.PLUG_FACADE.get();
                        output.accept(item.createItemStack(FacadeInstance.createSingle(info, false)));
                        output.accept(item.createItemStack(FacadeInstance.createSingle(info, true)));
                    }
                }
            })
            .build()
    );

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Facades need every block to be registered
            FacadeStateManager.init();
            FacadeAPI.facadeItem = BCSiliconItems.PLUG_FACADE.get();
            BCSiliconRecipes.register();
        });
    }
}
