/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.statements;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A parameter holding a single item (like the item that an inventory should contain). */
public class StatementParameterItemStack implements IStatementParameter {
    /** Immutable parameter that has the {@link ItemStack#EMPTY} as its {@link #stack}. */
    public static final StatementParameterItemStack EMPTY = new StatementParameterItemStack();

    @Nonnull
    protected final ItemStack stack;

    public StatementParameterItemStack() {
        stack = ItemStack.EMPTY;
    }

    public StatementParameterItemStack(@Nonnull ItemStack stack) {
        this.stack = stack;
    }

    public StatementParameterItemStack(CompoundTag nbt) {
        stack = ItemStack.of(nbt.getCompound("stack"));
    }

    @Override
    public void writeToNbt(CompoundTag compound) {
        if (!stack.isEmpty()) {
            compound.put("stack", stack.save(new CompoundTag()));
        }
    }

    @Override
    public ResourceLocation getSprite() {
        return null;
    }

    @Override
    @Nonnull
    public ItemStack getItemStack() {
        return stack;
    }

    @Override
    public StatementParameterItemStack onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
        StatementMouseClick mouse) {
        if (stack.isEmpty()) {
            return EMPTY;
        } else {
            return new StatementParameterItemStack(stack.copyWithCount(1));
        }
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof StatementParameterItemStack param && ItemStack.matches(stack, param.stack);
    }

    @Override
    public int hashCode() {
        return stack.isEmpty() ? 0 : stack.getItem().hashCode();
    }

    @Override
    public Component getDescription() {
        throw new UnsupportedOperationException("Don't call getDescription directly!");
    }

    @Override
    public List<Component> getTooltip() {
        if (stack.isEmpty()) {
            return List.of();
        }
        List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(null, TooltipFlag.NORMAL));
        for (int i = 1; i < tooltip.size(); i++) {
            tooltip.set(i, tooltip.get(i).copy().withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }

    @Override
    public String getUniqueTag() {
        return "buildcraft:stack";
    }

    @Override
    public IStatementParameter rotateLeft() {
        return this;
    }

    @Override
    public IStatementParameter[] getPossible(IStatementContainer source) {
        return null;
    }
}
