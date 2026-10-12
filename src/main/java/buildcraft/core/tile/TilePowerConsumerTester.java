/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;

import buildcraft.lib.tile.TileBC;

import buildcraft.core.BCCoreBlocks;

/** Development-only block that accepts any amount of MJ from anything, and reports what it received. */
public class TilePowerConsumerTester extends TileBC implements IMjRedstoneReceiver {
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(this);
    private long lastReceived;
    private long totalReceived;

    public TilePowerConsumerTester(BlockPos pos, BlockState state) {
        super(BCCoreBlocks.POWER_TESTER_TILE.get(), pos, state);
        caps.addProvider(mjCaps);
    }

    public long getTotalReceived() {
        return totalReceived;
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        lastReceived = nbt.getLong("last");
        totalReceived = nbt.getLong("total");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("last", lastReceived);
        nbt.putLong("total", totalReceived);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.literal(
                "Last received = " + MjAPI.formatMj(lastReceived) + " MJ, total received = " + MjAPI.formatMj(
                    totalReceived
                ) + " MJ"
            ), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // IMjReceiver

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return 100000 * MjAPI.MJ;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (!simulate) {
            lastReceived = microJoules;
            totalReceived += microJoules;
            markChunkDirty();
        }
        return 0;
    }
}
