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

/** Active when the inventory next to the gate is filled below a given level (with any item, or the given one). */
public class TriggerInventoryLevel extends BCStatement implements ITriggerExternal {
    public final TriggerType type;

    public TriggerInventoryLevel(TriggerType type) {
        super("buildcraft:inventorylevel." + type.name().toLowerCase(Locale.ROOT),
            "buildcraft.inventorylevel." + type.name().toLowerCase(Locale.ROOT),
            "buildcraft.filteredBuffer." + type.name().toLowerCase(Locale.ROOT));
        this.type = type;
    }

    @Override
    public int maxParameters() {
        return 1;
    }

    @Override
    public ResourceLocation getSprite() {
        return sprite("trigger_inventory_" + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("buildcraft.gate.trigger.inventorylevel.below", (int) (type.level * 100));
    }

    @Override
    public boolean isTriggerActive(BlockEntity tile, Direction side, IStatementContainer container,
        IStatementParameter[] parameters) {
        IItemHandler handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null);
        if (handler == null) {
            return false;
        }
        ItemStack searchStack = getParam(0, parameters, new StatementParameterItemStack()).getItemStack();
        int itemSpace = 0;
        int foundItems = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stackInSlot = handler.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                if (searchStack.isEmpty()) {
                    itemSpace += handler.getSlotLimit(slot);
                } else {
                    int count = Math.min(handler.getSlotLimit(slot), searchStack.getMaxStackSize());
                    ItemStack leftOver = handler.insertItem(slot, searchStack.copyWithCount(count), true);
                    itemSpace += count - leftOver.getCount();
                }
            } else if (searchStack.isEmpty() || ItemStack.isSameItemSameTags(searchStack, stackInSlot)) {
                itemSpace += Math.min(stackInSlot.getMaxStackSize(), handler.getSlotLimit(slot));
                foundItems += stackInSlot.getCount();
            }
        }
        return itemSpace > 0 && foundItems / (float) itemSpace < type.level;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }

    @Override
    public IStatement[] getPossible() {
        return BCCoreStatements.TRIGGER_INVENTORY_ALL;
    }

    public enum TriggerType {
        BELOW25(0.25F),
        BELOW50(0.5F),
        BELOW75(0.75F);

        public final float level;

        TriggerType(float level) {
            this.level = level;
        }
    }
}
