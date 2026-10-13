/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.statements;

import java.util.Locale;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.StatementParameterItemStack;

/** Checks whether the inventory next to the gate is empty, full, has some space, or contains some items. */
public class TriggerInventory extends BCStatement implements ITriggerExternal {
    public final State state;

    public TriggerInventory(State state) {
        super("buildcraft:inventory." + state.name().toLowerCase(Locale.ROOT),
            "buildcraft.inventory." + state.name().toLowerCase(Locale.ROOT));
        this.state = state;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_inventory_" + state.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public int maxParameters() {
        return state == State.CONTAINS || state == State.SPACE ? 1 : 0;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.inventory." + state.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean isTriggerActive(BlockEntity tile, Direction side, IStatementContainer container,
        IStatementParameter[] parameters) {
        ItemStack searchedStack = ItemStack.EMPTY;
        if (parameters != null && parameters.length >= 1 && parameters[0] != null) {
            searchedStack = parameters[0].getItemStack();
        }
        IItemHandler handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
        if (handler == null || handler.getSlots() == 0) {
            return false;
        }
        boolean foundItems = false;
        boolean foundSpace = false;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            boolean matches = searchedStack.isEmpty() || ItemStack.isSameItemSameTags(stack, searchedStack);
            foundItems |= !stack.isEmpty() && matches;
            foundSpace |= (stack.isEmpty() || (matches && stack.getCount() < stack.getMaxStackSize()))
                && (searchedStack.isEmpty() || handler.insertItem(i, searchedStack, true).isEmpty());
        }
        return switch (state) {
            case EMPTY -> !foundItems;
            case CONTAINS -> foundItems;
            case SPACE -> foundSpace;
            case FULL -> !foundSpace;
        };
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_INVENTORY_ALL;
    }

    public enum State {
        EMPTY,
        CONTAINS,
        SPACE,
        FULL
    }
}
