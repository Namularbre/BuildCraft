/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.statement;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IAction;
import buildcraft.api.statements.IActionExternal;
import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IActionInternalSided;
import buildcraft.api.statements.IActionReceptor;
import buildcraft.api.statements.IActionSingle;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;

/** Wrapper class around IActionInternal to allow for treating all actions as internal actions. It also provides the
 * background colour for sides. */
public abstract class ActionWrapper extends StatementWrapper implements IActionInternal {
    protected boolean isActive = false;

    public ActionWrapper(IAction delegate, EnumPipePart sourcePart) {
        super(delegate, sourcePart);
    }

    public IAction getDelegate() {
        return (IAction) delegate;
    }

    /** @param side The side that the action comes from, or null for internal actions. */
    public static ActionWrapper wrap(IStatement statement, @Nullable Direction side) {
        if (statement == null) {
            return null;
        } else if (statement instanceof ActionWrapper wrapper) {
            return wrapper;
        } else if (statement instanceof IActionInternal internal && side == null) {
            return new ActionWrapperInternal(internal);
        } else if (statement instanceof IActionInternalSided sided) {
            if (side == null) {
                throw new NullPointerException("side");
            }
            return new ActionWrapperInternalSided(sided, side);
        } else if (statement instanceof IActionExternal external) {
            if (side == null) {
                throw new NullPointerException("side");
            }
            return new ActionWrapperExternal(external, side);
        } else {
            throw new IllegalArgumentException("Unknown class or interface " + statement.getClass());
        }
    }

    @Override
    public ActionWrapper[] getPossible() {
        IStatement[] possible = delegate.getPossible();
        List<ActionWrapper> list = new ArrayList<>(possible.length + 5);
        for (IStatement statement : possible) {
            list.add(wrap(statement, sourcePart.face));
        }
        if (sourcePart != EnumPipePart.CENTER) {
            EnumPipePart part = sourcePart;
            for (int j = 0; j < 5; j++) {
                part = part.next();
                list.add(wrap(delegate, part.face));
            }
        }
        return list.toArray(new ActionWrapper[0]);
    }

    public void actionDeactivated(IStatementContainer source, IStatementParameter[] parameters) {
        isActive = false;
    }

    /** @return True if this is a single tick action that already fired (so it shouldn't fire again). */
    protected boolean skipSingleTick() {
        return isActive && delegate instanceof IActionSingle single && single.singleActionTick();
    }

    public static class ActionWrapperInternal extends ActionWrapper {
        public final IActionInternal action;

        public ActionWrapperInternal(IActionInternal action) {
            super(action, EnumPipePart.CENTER);
            this.action = action;
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (skipSingleTick()) {
                return;
            }
            action.actionActivate(source, parameters);
            isActive = true;
        }
    }

    public static class ActionWrapperInternalSided extends ActionWrapper {
        public final IActionInternalSided action;

        public ActionWrapperInternalSided(IActionInternalSided action, @Nonnull Direction side) {
            super(action, EnumPipePart.fromFacing(side));
            this.action = action;
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (skipSingleTick()) {
                return;
            }
            action.actionActivate(sourcePart.face, source, parameters);
            isActive = true;
        }
    }

    public static class ActionWrapperExternal extends ActionWrapper {
        public final IActionExternal action;

        public ActionWrapperExternal(IActionExternal action, @Nonnull Direction side) {
            super(action, EnumPipePart.fromFacing(side));
            this.action = action;
        }

        @Override
        public void actionActivate(IStatementContainer source, IStatementParameter[] parameters) {
            if (skipSingleTick()) {
                return;
            }
            BlockEntity neighbourTile = getNeighbourTile(source);
            if (neighbourTile == null) {
                return;
            }
            action.actionActivate(neighbourTile, sourcePart.face, source, parameters);
            if (neighbourTile instanceof IActionReceptor receptor) {
                receptor.actionActivated(action, parameters);
            }
            isActive = true;
        }
    }
}
