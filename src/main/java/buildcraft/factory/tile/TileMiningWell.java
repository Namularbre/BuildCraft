/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.items.wrapper.EmptyHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.inventory.InventoryUtil;
import buildcraft.lib.mj.MjBatteryReceiver;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryConfig;

/** Breaks the blocks below it, one at a time, and sends what they drop to the pipes and inventories next to it. */
public class TileMiningWell extends TileMiner {
    /** How many ticks to wait before looking for blocks again, when there wasn't any to break. */
    private static final int RECHECK_DELAY = 20;

    private long nextCheck;

    public TileMiningWell(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.MINING_WELL_TILE.get(), pos, state);
        // Nothing can be taken out or put in, but item pipes connect to it (to get what it mines)
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, EmptyHandler.INSTANCE, EnumPipePart.VALUES);
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjBatteryReceiver(battery);
    }

    /** @return The power needed to break the block at the given position. */
    public static long getBreakPower(ServerLevel level, BlockPos pos) {
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        return (long) Math.floor(16 * MjAPI.MJ * ((hardness + 1) * 2) * BCFactoryConfig.miningMultiplier());
    }

    @Override
    protected void mine() {
        ServerLevel serverLevel = (ServerLevel) level;
        if (currentPos != null && canBreak(currentPos)) {
            long target = getBreakPower(serverLevel, currentPos);
            progress += battery.extractPower(0, target - progress);
            if (progress >= target) {
                progress = 0;
                level.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
                breakBlock(serverLevel, currentPos);
                nextPos();
            } else {
                level.destroyBlockProgress(currentPos.hashCode(), currentPos, (int) ((progress * 9) / target));
            }
        } else if (currentPos != null || level.getGameTime() >= nextCheck) {
            // The block changed (or there wasn't one): look again
            nextCheck = level.getGameTime() + RECHECK_DELAY;
            progress = 0;
            nextPos();
        }
    }

    /** Breaks the block like a player with a diamond pickaxe would, if no other mod stops it. */
    private void breakBlock(ServerLevel serverLevel, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, getOwner());
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, blockPos, state, fakePlayer);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(
            state, serverLevel, blockPos, level.getBlockEntity(blockPos), fakePlayer, tool
        );
        level.destroyBlock(blockPos, false, fakePlayer);
        for (ItemStack stack : drops) {
            InventoryUtil.addToBestAcceptor(level, worldPosition, stack);
        }
    }

    private boolean canBreak(BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir() || state.getDestroySpeed(level, blockPos) < 0) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            // Mining wells go through water, but not through anything thicker (like lava)
            return state.getFluidState().getFluidType().getViscosity() <= 1000;
        }
        return true;
    }

    private void nextPos() {
        int maxDepth = BCFactoryConfig.miningMaxDepth();
        for (BlockPos pos = worldPosition.below(); !level.isOutsideBuildHeight(pos); pos = pos.below()) {
            if (worldPosition.getY() - pos.getY() > maxDepth) {
                break;
            }
            if (canBreak(pos)) {
                currentPos = pos;
                updateLength();
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(BCFactoryBlocks.TUBE.get())) {
                break;
            }
        }
        currentPos = null;
        updateLength();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && currentPos != null) {
            level.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
        }
    }
}
