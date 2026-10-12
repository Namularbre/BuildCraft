/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.wrapper.EmptyHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.inventory.InventoryUtil;
import buildcraft.lib.misc.BlockUtil;
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

    private void breakBlock(ServerLevel serverLevel, BlockPos blockPos) {
        BlockUtil.breakBlockAndGetDrops(serverLevel, blockPos, getOwner()).ifPresent(
            drops -> drops.forEach(stack -> InventoryUtil.addToBestAcceptor(level, worldPosition, stack))
        );
    }

    private boolean canBreak(BlockPos blockPos) {
        return BlockUtil.canMachineBreak(level, blockPos);
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
