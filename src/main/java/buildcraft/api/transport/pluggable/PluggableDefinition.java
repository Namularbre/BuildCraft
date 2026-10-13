/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.transport.pluggable;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.transport.pipe.IPipeHolder;

/** A kind of pluggable: how to create it, save it and send it to clients. */
public final class PluggableDefinition {
    public final ResourceLocation identifier;
    public final IPluggableNetLoader loader;
    public final IPluggableNbtReader reader;
    @Nullable
    public final IPluggableCreator creator;

    public PluggableDefinition(ResourceLocation identifier, IPluggableNbtReader reader, IPluggableNetLoader loader) {
        this.identifier = identifier;
        this.reader = reader;
        this.loader = loader;
        this.creator = null;
    }

    public PluggableDefinition(ResourceLocation identifier, IPluggableCreator creator) {
        this.identifier = identifier;
        this.reader = creator;
        this.loader = creator;
        this.creator = creator;
    }

    public PipePluggable readFromNbt(IPipeHolder holder, Direction side, CompoundTag nbt) {
        return reader.readFromNbt(this, holder, side, nbt);
    }

    public PipePluggable loadFromBuffer(IPipeHolder holder, Direction side, FriendlyByteBuf buffer) {
        return loader.loadFromBuffer(this, holder, side, buffer);
    }

    @FunctionalInterface
    public interface IPluggableNbtReader {
        /** Reads the pluggable from NBT, making a best effort (falling back to defaults) if the data is wrong. */
        PipePluggable readFromNbt(PluggableDefinition definition, IPipeHolder holder, Direction side, CompoundTag nbt);
    }

    @FunctionalInterface
    public interface IPluggableNetLoader {
        PipePluggable loadFromBuffer(PluggableDefinition definition, IPipeHolder holder, Direction side,
            FriendlyByteBuf buffer);
    }

    /** For pluggables without any data. */
    @FunctionalInterface
    public interface IPluggableCreator extends IPluggableNbtReader, IPluggableNetLoader {
        @Override
        default PipePluggable loadFromBuffer(PluggableDefinition definition, IPipeHolder holder, Direction side,
            FriendlyByteBuf buffer) {
            return createSimplePluggable(definition, holder, side);
        }

        @Override
        default PipePluggable readFromNbt(PluggableDefinition definition, IPipeHolder holder, Direction side,
            CompoundTag nbt) {
            return createSimplePluggable(definition, holder, side);
        }

        PipePluggable createSimplePluggable(PluggableDefinition definition, IPipeHolder holder, Direction side);
    }
}
