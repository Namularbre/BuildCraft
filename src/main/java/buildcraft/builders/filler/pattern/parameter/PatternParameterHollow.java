/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.filler.pattern.parameter;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;

import buildcraft.builders.filler.FillerSprites;



public enum PatternParameterHollow implements IStatementParameter {
    FILLED_INNER(true, false),
    FILLED_OUTER(true, true),
    HOLLOW(false, false);

    public final boolean filled;
    public final boolean outerFilled;

    PatternParameterHollow(boolean filled, boolean outerFilled) {
        this.filled = filled;
        this.outerFilled = outerFilled;
    }

    public static PatternParameterHollow readFromNbt(CompoundTag nbt) {
        if (nbt.getBoolean("filled")) {
            if (nbt.getBoolean("outer")) {
                return FILLED_OUTER;
            } else {
                return FILLED_INNER;
            }
        }
        return HOLLOW;
    }

    @Override
    public void writeToNbt(CompoundTag compound) {
        compound.putBoolean("filled", filled);
        if (filled) {
            compound.putBoolean("outer", outerFilled);
        }
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:fillerParameterHollow";
    }

    @Override
    public ResourceLocation getSprite() {
        if (filled) {
            if (outerFilled) {
                return FillerSprites.param("filled_outer");
            } else {
                return FillerSprites.param("filled_inner");
            }
        }
        return FillerSprites.param("hollow");
    }

    @Nonnull
    @Override
    public ItemStack getItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public Component getDescription() {
        String after = filled ? (outerFilled ? "filled_outer" : "filled") : "hollow";
        return Component.translatable("buildcraft.fillerpattern.parameter." + after);
    }

    @Override
    public PatternParameterHollow onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
        StatementMouseClick mouse) {
        return null;
    }

    @Override
    public IStatementParameter rotateLeft() {
        return this;
    }

    @Override
    public PatternParameterHollow[] getPossible(IStatementContainer source) {
        return values();
    }
}
