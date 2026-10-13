/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import buildcraft.api.core.IConvertable;

/** Something that can be shown in a slot of a gate GUI: a statement, or one of its parameters. */
public interface IGuiSlot extends IConvertable {
    /** Every statement needs a unique tag, it should be in the format of "&lt;modid&gt;:&lt;name&gt;".
     *
     * @return the unique id */
    String getUniqueTag();

    /** Return the description in the UI. Note that this should NEVER be called directly, instead this acts as a bridge
     * for {@link #getTooltip()}. (As such this might return null or throw an exception) */
    Component getDescription();

    /** @return The full tooltip for the UI. */
    default List<Component> getTooltip() {
        Component desc = getDescription();
        if (desc == null) {
            return List.of();
        }
        return List.of(desc);
    }

    /** @return The texture (a whole 16x16 png, like "buildcraft:textures/gui/triggers/trigger_true.png") to show in a
     *         GUI, or null if this should not render a sprite. */
    @Nullable
    ResourceLocation getSprite();
}
