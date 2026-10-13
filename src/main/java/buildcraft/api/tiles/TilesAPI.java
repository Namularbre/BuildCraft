/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.tiles;

import javax.annotation.Nonnull;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class TilesAPI {
    @Nonnull
    public static final Capability<IControllable> CAP_CONTROLLABLE = CapabilityManager.get(new CapabilityToken<>() {});

    @Nonnull
    public static final Capability<IHasWork> CAP_HAS_WORK = CapabilityManager.get(new CapabilityToken<>() {});

    private TilesAPI() {}

    /** Registers the capabilities. Must be called from a {@link RegisterCapabilitiesEvent} listener. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IControllable.class);
        event.register(IHasWork.class);
    }
}
