/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.api.core.BCLog;
import buildcraft.api.facades.IFacade;
import buildcraft.api.facades.IFacadePhasedState;
import buildcraft.api.facades.IFacadeRegistry;
import buildcraft.api.facades.IFacadeState;

/** Finds every block state that facades can look like: full, plain blocks (without a block entity, and not fluids). */
public enum FacadeStateManager implements IFacadeRegistry {
    INSTANCE;

    /** Every valid state, in registry order. */
    public static final Map<BlockState, FacadeBlockStateInfo> validFacadeStates = new LinkedHashMap<>();
    /** The facades that each item makes. */
    public static final Map<Item, List<FacadeBlockStateInfo>> stackFacades = new HashMap<>();
    /** Used for facades whose state isn't valid anymore (or couldn't be read). */
    public static FacadeBlockStateInfo defaultState = new FacadeBlockStateInfo(Blocks.AIR.defaultBlockState(),
        ItemStack.EMPTY, false);

    /** @return The info of the given state, or {@link #defaultState} if facades can't look like it. */
    public static FacadeBlockStateInfo getInfo(BlockState state) {
        return validFacadeStates.getOrDefault(state, defaultState);
    }

    @Nullable
    public static FacadeBlockStateInfo getInfoForBlock(Block block) {
        return validFacadeStates.get(block.defaultBlockState());
    }

    /** Must be called once every block is registered. */
    public static void init() {
        validFacadeStates.clear();
        stackFacades.clear();
        for (Block block : ForgeRegistries.BLOCKS) {
            try {
                scanBlock(block);
            } catch (RuntimeException e) {
                BCLog.logger.warn("[silicon.facade] Skipping {} as something about it threw an exception!", block, e);
            }
        }
        BCLog.logger.info("[silicon.facade] Found {} facade states, {} of which can be made", validFacadeStates.size(),
            validFacadeStates.values().stream().filter(info -> info.isVisible).count());
    }

    private static void scanBlock(Block block) {
        if (block instanceof LiquidBlock || block instanceof EntityBlock || block == Blocks.AIR) {
            return;
        }
        Item item = block.asItem();
        ItemStack requiredStack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            if (state.getRenderShape() != RenderShape.MODEL
                || !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                continue;
            }
            FacadeBlockStateInfo info = new FacadeBlockStateInfo(state, requiredStack, isMadeFacade(state));
            validFacadeStates.put(state, info);
            if (info.isVisible) {
                stackFacades.computeIfAbsent(item, k -> new ArrayList<>()).add(info);
            }
        }
    }

    /** Blocks have many states that look the same (like the distance of leaves, or the note of note blocks), so only
     * the default state of each block, and the ones turned another way (like logs), can be made into facades.
     *
     * @return True if every property of the state has its default value, or is a direction or an axis. */
    private static boolean isMadeFacade(BlockState state) {
        BlockState defaultState = state.getBlock().defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            Class<?> type = property.getValueClass();
            if (type != Direction.class && type != Direction.Axis.class
                && !state.getValue(property).equals(defaultState.getValue(property))) {
                return false;
            }
        }
        return true;
    }

    // IFacadeRegistry

    @Override
    public Collection<? extends IFacadeState> getValidFacades() {
        return Collections.unmodifiableCollection(validFacadeStates.values());
    }

    @Override
    public IFacadePhasedState createPhasedState(IFacadeState state, @Nullable DyeColor activeColor) {
        return new FacadePhasedState((FacadeBlockStateInfo) state, activeColor);
    }

    @Override
    public IFacade createPhasedFacade(IFacadePhasedState[] states, boolean isHollow) {
        FacadePhasedState[] realStates = new FacadePhasedState[states.length];
        for (int i = 0; i < states.length; i++) {
            realStates[i] = (FacadePhasedState) states[i];
        }
        return new FacadeInstance(realStates, isHollow);
    }
}
