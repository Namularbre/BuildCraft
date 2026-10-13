/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraftforge.eventbus.api.IEventBus;

import buildcraft.api.transport.pipe.PipeApi;

/** The transport module: pipes. */
public final class BCTransport {
    private BCTransport() {}

    public static void init(IEventBus modBus) {
        BCTransportPipes.init();
        BCTransportPlugs.init();
        BCTransportBlocks.init();
        BCTransportItems.init();
        BCTransportMenus.init();
        modBus.addListener(PipeApi::registerCapabilities);
    }
}
