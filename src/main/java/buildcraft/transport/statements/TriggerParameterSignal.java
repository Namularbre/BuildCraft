/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.gates.IGate;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;

/** Another wire colour that {@link TriggerPipeSignal} checks: it must be powered (or not). */
public class TriggerParameterSignal implements IStatementParameter {
    public static final TriggerParameterSignal EMPTY = new TriggerParameterSignal(false, null);

    public final boolean active;
    @Nullable
    public final DyeColor colour;

    private TriggerParameterSignal(boolean active, @Nullable DyeColor colour) {
        this.active = active;
        this.colour = colour;
    }

    public static TriggerParameterSignal get(boolean active, @Nullable DyeColor colour) {
        return colour == null ? EMPTY : new TriggerParameterSignal(active, colour);
    }

    public static TriggerParameterSignal readFromNbt(CompoundTag nbt) {
        if (nbt.contains("color", Tag.TAG_ANY_NUMERIC)) {
            return get(nbt.getBoolean("active"), DyeColor.byId(nbt.getByte("color")));
        }
        return EMPTY;
    }

    @Override
    public void writeToNbt(CompoundTag nbt) {
        if (colour != null) {
            nbt.putByte("color", (byte) colour.getId());
            nbt.putBoolean("active", active);
        }
    }

    public static TriggerParameterSignal readFromBuf(FriendlyByteBuf buffer) {
        int colour = buffer.readByte();
        if (colour < 0) {
            return EMPTY;
        }
        return get(buffer.readBoolean(), DyeColor.byId(colour));
    }

    @Override
    public void writeToBuf(FriendlyByteBuf buffer) {
        buffer.writeByte(colour == null ? -1 : colour.getId());
        if (colour != null) {
            buffer.writeBoolean(active);
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
        return colour == null ? null : TriggerPipeSignal.getSprite(active, colour);
    }

    @Override
    public Component getDescription() {
        return colour == null ? null : TriggerPipeSignal.getDescription(active, colour);
    }

    @Override
    public TriggerParameterSignal onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
        StatementMouseClick mouse) {
        return null;
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:pipeWireTrigger";
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
        List<TriggerParameterSignal> poss = new ArrayList<>();
        poss.add(EMPTY);
        for (DyeColor c : DyeColor.values()) {
            if (TriggerPipeSignal.doesGateHaveColour(gate, c)) {
                poss.add(get(true, c));
                poss.add(get(false, c));
            }
        }
        return poss.toArray(new IStatementParameter[0]);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TriggerParameterSignal other && other.active == active && other.colour == colour;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, colour);
    }
}
