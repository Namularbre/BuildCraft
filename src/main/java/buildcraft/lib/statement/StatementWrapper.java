/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IAction;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITrigger;

/** A statement, and the side of the container that it comes from (for external and sided statements). */
public abstract class StatementWrapper implements IStatement, Comparable<StatementWrapper> {
    public final IStatement delegate;
    /** Used to determine the background colour of triggers and actions. */
    public final EnumPipePart sourcePart;

    public StatementWrapper(IStatement delegate, EnumPipePart sourcePart) {
        this.delegate = delegate;
        this.sourcePart = sourcePart;
    }

    @Override
    public String getUniqueTag() {
        return delegate.getUniqueTag();
    }

    @Override
    public int maxParameters() {
        return delegate.maxParameters();
    }

    @Override
    public int minParameters() {
        return delegate.minParameters();
    }

    @Override
    public Component getDescription() {
        return delegate.getDescription();
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return delegate.createParameter(index);
    }

    @Override
    public IStatementParameter createParameter(IStatementParameter old, int index) {
        return delegate.createParameter(old, index);
    }

    @Override
    public IStatement rotateLeft() {
        return delegate.rotateLeft();
    }

    @Override
    public ResourceLocation getSprite() {
        return delegate.getSprite();
    }

    public BlockEntity getNeighbourTile(IStatementContainer source) {
        return source.getNeighbourTile(sourcePart.face);
    }

    @Override
    public abstract StatementWrapper[] getPossible();

    @Override
    public boolean isPossibleOrdered() {
        return delegate.isPossibleOrdered();
    }

    @Override
    public List<Component> getTooltip() {
        List<Component> list = delegate.getTooltip();
        if (sourcePart != EnumPipePart.CENTER) {
            list = new ArrayList<>(list);
            Component side = Component.translatable("buildcraft.direction." + sourcePart.face.getSerializedName());
            list.add(Component.translatable("buildcraft.gate.side", side));
        }
        return list;
    }

    @Override
    public <T> T convertTo(Class<T> clazz) {
        T t = delegate.convertTo(clazz);
        if (t != null) {
            return t;
        }
        // As we need to keep the wrapper it's not quite as simple as "return t;"
        if (clazz.isAssignableFrom(TriggerWrapper.class)) {
            ITrigger trigger = delegate.convertTo(ITrigger.class);
            if (trigger != null) {
                return clazz.cast(TriggerWrapper.wrap(trigger, sourcePart.face));
            }
        } else if (clazz.isAssignableFrom(ActionWrapper.class)) {
            IAction action = delegate.convertTo(IAction.class);
            if (action != null) {
                return clazz.cast(ActionWrapper.wrap(action, sourcePart.face));
            }
        }
        return null;
    }

    @Override
    public int compareTo(StatementWrapper o) {
        if (sourcePart != o.sourcePart) {
            return Integer.compare(o.sourcePart.getIndex(), sourcePart.getIndex());
        }
        if (delegate == o.delegate) {
            return 0;
        }
        if (delegate.getClass() == o.delegate.getClass()) {
            IStatement[] poss = delegate.getPossible();
            IStatement[] oPoss = o.delegate.getPossible();
            if (Arrays.equals(poss, oPoss)) {
                int idxThis = -1;
                int idxThat = -1;
                for (int i = 0; i < poss.length; i++) {
                    if (poss[i] == delegate) {
                        idxThis = i;
                    }
                    if (poss[i] == o.delegate) {
                        idxThat = i;
                    }
                }
                if (idxThis != idxThat && idxThis != -1 && idxThat != -1) {
                    return Integer.compare(idxThis, idxThat);
                }
            }
        }
        return getUniqueTag().compareTo(o.getUniqueTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourcePart, getUniqueTag());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        StatementWrapper other = (StatementWrapper) obj;
        return sourcePart == other.sourcePart && getUniqueTag().equals(other.getUniqueTag());
    }
}
