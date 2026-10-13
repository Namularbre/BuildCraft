/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;

/** Another wire colour that {@link ActionPipeSignal} powers. */
public class ActionParameterSignal implements IStatementParameter {
    public static final ActionParameterSignal EMPTY = new ActionParameterSignal(null);
    private static final Map<DyeColor, ActionParameterSignal> SIGNALS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor colour : DyeColor.values()) {
            SIGNALS.put(colour, new ActionParameterSignal(colour));
        }
    }

    @Nullable
    public final DyeColor colour;

    private ActionParameterSignal(@Nullable DyeColor colour) {
        this.colour = colour;
    }

    public static ActionParameterSignal get(@Nullable DyeColor colour) {
        return colour == null ? EMPTY : SIGNALS.get(colour);
    }

    public static ActionParameterSignal readFromNbt(CompoundTag nbt) {
        if (nbt.contains("color", Tag.TAG_ANY_NUMERIC)) {
            return get(DyeColor.byId(nbt.getByte("color")));
        }
        return EMPTY;
    }

    @Override
    public void writeToNbt(CompoundTag nbt) {
        if (colour != null) {
            nbt.putByte("color", (byte) colour.getId());
        }
    }

    @Nonnull
    @Override
    public ItemStack getItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public DrawType getDrawType() {
        return DrawType.SPRITE_ONLY;
    }

    @Override
    public ResourceLocation getSprite() {
        return colour == null ? null : TriggerPipeSignal.getSprite(true, colour);
    }

    @Override
    public Component getDescription() {
        return colour == null ? null : ActionPipeSignal.getDescription(colour);
    }

    @Override
    public ActionParameterSignal onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
        StatementMouseClick mouse) {
        return null;
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:pipeWireAction";
    }

    @Override
    public IStatementParameter rotateLeft() {
        return this;
    }

    @Override
    public IStatementParameter[] getPossible(IStatementContainer source) {
        if (!(source instanceof IGate gate)) {
            return null;
        }
        List<IStatementParameter> poss = new ArrayList<>();
        poss.add(EMPTY);
        for (DyeColor c : DyeColor.values()) {
            if (TriggerPipeSignal.doesGateHaveColour(gate, c)) {
                poss.add(get(c));
            }
        }
        return poss.toArray(new IStatementParameter[0]);
    }
}
