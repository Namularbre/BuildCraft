package buildcraft.api.transport.pipe;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface IPipeRegistry {
    PipeDefinition getDefinition(ResourceLocation identifier);

    void registerPipe(PipeDefinition definition);

    /** Maps the given {@link PipeDefinition} to an {@link IItemPipe}. This acts exactly akin to
     * {@link java.util.Map#put(Object, Object)}. Pipe items are normal items, registered like any other. */
    void setItemForPipe(PipeDefinition definition, @Nullable IItemPipe item);

    IItemPipe getItemForPipe(PipeDefinition definition);

    Iterable<PipeDefinition> getAllRegisteredPipes();
}
