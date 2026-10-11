/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.cap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import buildcraft.api.core.EnumPipePart;

/** Maps capabilities to the objects that implement them, per side. Every {@link LazyOptional} handed out is cached, so
 * {@link #invalidate()} must be called when the owner is removed, or when a getter would start returning a different
 * object. */
public class CapabilityHelper implements ICapabilityProvider {
    private final Map<EnumPipePart, Map<Capability<?>, Supplier<?>>> caps = new EnumMap<>(EnumPipePart.class);
    private final Map<EnumPipePart, Map<Capability<?>, LazyOptional<?>>> optionals = new EnumMap<>(EnumPipePart.class);
    private final List<ICapabilityProvider> additional = new ArrayList<>();

    public CapabilityHelper() {
        for (EnumPipePart face : EnumPipePart.VALUES) {
            caps.put(face, new HashMap<>());
            optionals.put(face, new HashMap<>());
        }
    }

    public <T> void addCapabilityInstance(@Nullable Capability<T> cap, T instance, EnumPipePart... parts) {
        Supplier<T> supplier = () -> instance;
        addCapability(cap, supplier, parts);
    }

    public <T> void addCapability(@Nullable Capability<T> cap, Supplier<T> getter, EnumPipePart... parts) {
        if (cap == null) {
            return;
        }
        for (EnumPipePart part : parts) {
            caps.get(part).put(cap, getter);
        }
    }

    public <T> void addCapability(@Nullable Capability<T> cap, Function<Direction, T> getter, EnumPipePart... parts) {
        if (cap == null) {
            return;
        }
        for (EnumPipePart part : parts) {
            caps.get(part).put(cap, () -> getter.apply(part.face));
        }
    }

    public <T extends ICapabilityProvider> T addProvider(T provider) {
        if (provider != null) {
            additional.add(provider);
        }
        return provider;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        EnumPipePart part = EnumPipePart.fromFacing(facing);
        Supplier<?> supplier = caps.get(part).get(capability);
        if (supplier != null) {
            Map<Capability<?>, LazyOptional<?>> cache = optionals.get(part);
            LazyOptional<?> optional = cache.get(capability);
            if (optional == null || !optional.isPresent()) {
                Object value = supplier.get();
                if (value == null) {
                    return LazyOptional.empty();
                }
                optional = LazyOptional.of(() -> value);
                cache.put(capability, optional);
            }
            return optional.cast();
        }
        for (ICapabilityProvider provider : additional) {
            LazyOptional<T> optional = provider.getCapability(capability, facing);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return LazyOptional.empty();
    }

    /** Invalidates every capability that has been handed out so far. Later queries create fresh ones. */
    public void invalidate() {
        for (Map<Capability<?>, LazyOptional<?>> cache : optionals.values()) {
            for (LazyOptional<?> optional : cache.values()) {
                optional.invalidate();
            }
            cache.clear();
        }
    }
}
