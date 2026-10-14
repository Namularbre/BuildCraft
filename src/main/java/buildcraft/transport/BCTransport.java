/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import buildcraft.api.transport.pipe.PipeApi;

import buildcraft.transport.statements.BCTransportStatements;
import buildcraft.transport.stripes.PipeExtensionManager;
import buildcraft.transport.stripes.StripesHandlers;
import buildcraft.transport.stripes.StripesRegistry;

/** The transport module: pipes. */
public final class BCTransport {
    private BCTransport() {}

    public static void init(IEventBus modBus) {
        BCTransportPipes.init();
        BCTransportPlugs.init();
        BCTransportStatements.init();
        BCTransportBlocks.init();
        BCTransportItems.init();
        BCTransportMenus.init();
        PipeApi.stripeRegistry = StripesRegistry.INSTANCE;
        PipeApi.extensionManager = PipeExtensionManager.INSTANCE;
        StripesHandlers.register(PipeApi.stripeRegistry);
        PipeApi.extensionManager.registerRetractionPipe(BCTransportPipes.VOID_ITEM);
        MinecraftForge.EVENT_BUS.register(PipeExtensionManager.INSTANCE);
        modBus.addListener(PipeApi::registerCapabilities);
    }
}
