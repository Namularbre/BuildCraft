/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadeItem;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadePhasedState;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.silicon.plug.PluggableFacade;

/** A facade, which looks like the block that it was made from (its states are stored in the stack's tag). */
public class ItemPluggableFacade extends Item implements IItemPluggable, IFacadeItem {
    public ItemPluggableFacade(Properties properties) {
        super(properties);
    }

    @Nonnull
    public ItemStack createItemStack(FacadeInstance state) {
        ItemStack item = new ItemStack(this);
        item.getOrCreateTag().put("facade", state.writeToNbt());
        return item;
    }

    public static FacadeInstance getStates(@Nonnull ItemStack item) {
        return FacadeInstance.readFromNbt(item.getOrCreateTag().getCompound("facade"));
    }

    @Nonnull
    @Override
    public ItemStack getFacadeForBlock(BlockState state) {
        FacadeBlockStateInfo info = FacadeStateManager.validFacadeStates.get(state);
        return info == null ? ItemStack.EMPTY : createItemStack(FacadeInstance.createSingle(info, false));
    }

    @Nullable
    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        FacadeInstance states = getStates(stack);
        if (!states.phasedStates[0].stateInfo.isVisible) {
            return null;
        }
        SoundType sound = states.phasedStates[0].stateInfo.state.getSoundType();
        holder.getPipeWorld().playSound(null, holder.getPipePos(), sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1) / 2, sound.getPitch() * 0.8f);
        return new PluggableFacade(BCSiliconPlugs.FACADE, holder, side, states);
    }

    @Override
    public Component getName(ItemStack stack) {
        FacadeInstance states = getStates(stack);
        String key = states.isHollow ? "buildcraft.facade.hollow" : "buildcraft.facade";
        if (states.phasedStates.length > 1) {
            return Component.translatable("buildcraft.facade.phased");
        }
        return Component.translatable(key, getStateDisplayName(states.phasedStates[0]));
    }

    public static Component getStateDisplayName(FacadePhasedState state) {
        ItemStack stack = state.stateInfo.requiredStack;
        return stack.isEmpty() ? Component.translatable("buildcraft.facade.unknown") : stack.getHoverName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FacadeInstance states = getStates(stack);
        BlockState state = states.phasedStates[0].stateInfo.state;
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())))
                .withStyle(ChatFormatting.DARK_GRAY));
        }
        // Show the properties that tell apart the facades that come from the same item (like the axis of logs)
        List<FacadeBlockStateInfo> siblings = FacadeStateManager.stackFacades.get(state.getBlock().asItem());
        if (siblings != null && siblings.size() > 1) {
            state.getValues().forEach((property, value) -> tooltip.add(Component.literal(
                property.getName() + " = " + value.toString()
            ).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        }
    }

    // IFacadeItem

    @Override
    public ItemStack createFacadeStack(IFacade facade) {
        return createItemStack((FacadeInstance) facade);
    }

    @Override
    public IFacade getFacade(@Nonnull ItemStack facade) {
        return getStates(facade);
    }
}
