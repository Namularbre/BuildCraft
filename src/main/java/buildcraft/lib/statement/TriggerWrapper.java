/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerExternalOverride;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;

/** Wrapper class around ITriggerInternal to allow for treating all triggers as internal triggers. It also provides the
 * background colour for sides. */
public abstract class TriggerWrapper extends StatementWrapper implements ITriggerInternal {
    public TriggerWrapper(IStatement delegate, EnumPipePart sourcePart) {
        super(delegate, sourcePart);
    }

    /** @param side The side that the trigger comes from, or null for internal triggers. */
    public static TriggerWrapper wrap(IStatement statement, @Nullable Direction side) {
        if (statement == null) {
            return null;
        } else if (statement instanceof TriggerWrapper wrapper) {
            return wrapper;
        } else if (statement instanceof ITriggerInternal internal && side == null) {
            return new TriggerWrapperInternal(internal);
        } else if (statement instanceof ITriggerInternalSided sided) {
            if (side == null) {
                throw new NullPointerException("side");
            }
            return new TriggerWrapperInternalSided(sided, side);
        } else if (statement instanceof ITriggerExternal external) {
            if (side == null) {
                throw new NullPointerException("side");
            }
            return new TriggerWrapperExternal(external, side);
        } else {
            throw new IllegalArgumentException("Unknown class or interface " + statement.getClass());
        }
    }

    @Override
    public TriggerWrapper[] getPossible() {
        IStatement[] possible = delegate.getPossible();
        boolean andSides = sourcePart != EnumPipePart.CENTER;
        TriggerWrapper[] real = new TriggerWrapper[possible.length + (andSides ? 5 : 0)];
        for (int i = 0; i < possible.length; i++) {
            real[i] = wrap(possible[i], sourcePart.face);
        }
        if (andSides) {
            EnumPipePart part = sourcePart;
            for (int j = 0; j < 5; j++) {
                part = part.next();
                real[j + possible.length] = wrap(delegate, part.face);
            }
        }
        return real;
    }

    public static class TriggerWrapperInternal extends TriggerWrapper {
        public final ITriggerInternal trigger;

        public TriggerWrapperInternal(ITriggerInternal trigger) {
            super(trigger, EnumPipePart.CENTER);
            this.trigger = trigger;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            return trigger.isTriggerActive(source, parameters);
        }
    }

    public static class TriggerWrapperInternalSided extends TriggerWrapper {
        public final ITriggerInternalSided trigger;

        public TriggerWrapperInternalSided(ITriggerInternalSided trigger, @Nonnull Direction side) {
            super(trigger, EnumPipePart.fromFacing(side));
            this.trigger = trigger;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            return trigger.isTriggerActive(sourcePart.face, source, parameters);
        }
    }

    public static class TriggerWrapperExternal extends TriggerWrapper {
        public final ITriggerExternal trigger;

        public TriggerWrapperExternal(ITriggerExternal trigger, @Nonnull Direction side) {
            super(trigger, EnumPipePart.fromFacing(side));
            this.trigger = trigger;
        }

        @Override
        public boolean isTriggerActive(IStatementContainer source, IStatementParameter[] parameters) {
            BlockEntity tile = getNeighbourTile(source);
            if (tile == null) {
                return false;
            }
            if (tile instanceof ITriggerExternalOverride override) {
                ITriggerExternalOverride.Result result =
                    override.override(sourcePart.face, source, trigger, parameters);
                if (result == ITriggerExternalOverride.Result.FALSE) {
                    return false;
                } else if (result == ITriggerExternalOverride.Result.TRUE) {
                    return true;
                }
            }
            return trigger.isTriggerActive(tile, sourcePart.face, source, parameters);
        }
    }
}
