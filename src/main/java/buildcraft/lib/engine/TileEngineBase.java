/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IItemPipe;

import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

/** Base class for every engine. Engines build up heat and power, and push it out of the side they face (the
 * {@link BlockEngineBase#FACING} property) in pulses, as the piston moves. */
public abstract class TileEngineBase extends TileBC implements ITickable {

    /** Heat per {@link MjAPI#MJ}. */
    public static final double HEAT_PER_MJ = 0.0023;

    public static final double MIN_HEAT = 20;
    public static final double IDEAL_HEAT = 100;
    public static final double MAX_HEAT = 250;

    @Nonnull
    public final IMjConnector mjConnector = createConnector();
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(mjConnector);

    protected double heat = MIN_HEAT;
    protected long power = 0;
    private long lastPower = 0;
    /** Increments from 0 to 1. Above 0.5 all of the held power is emitted. */
    private float progress, lastProgress;
    private int progressPart = 0;

    public long currentOutput;
    public boolean isRedstonePowered = false;
    protected boolean isPumping = false;

    public TileEngineBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ##################
    //
    // Saving and syncing
    //
    // ##################

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        isRedstonePowered = nbt.getBoolean("isRedstonePowered");
        heat = nbt.getDouble("heat");
        power = nbt.getLong("power");
        progress = nbt.getFloat("progress");
        progressPart = nbt.getInt("progressPart");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("isRedstonePowered", isRedstonePowered);
        nbt.putDouble("heat", heat);
        nbt.putLong("power", power);
        nbt.putFloat("progress", progress);
        nbt.putInt("progressPart", progressPart);
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                isPumping = buffer.readBoolean();
                progress = buffer.readFloat();
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                heat = buffer.readFloat();
                currentOutput = buffer.readLong();
                power = buffer.readLong();
            }
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                buffer.writeBoolean(isPumping);
                buffer.writeFloat(progress);
            } else if (id == NET_GUI_DATA || id == NET_GUI_TICK) {
                buffer.writeFloat((float) heat);
                buffer.writeLong(currentOutput);
                buffer.writeLong(power);
            }
        }
    }

    // ##################
    //
    // Facing
    //
    // ##################

    public Direction getCurrentFacing() {
        return getBlockState().getValue(BlockEngineBase.FACING);
    }

    private void setCurrentFacing(Direction facing) {
        BlockState state = getBlockState();
        if (state.getValue(BlockEngineBase.FACING) != facing) {
            level.setBlock(worldPosition, state.setValue(BlockEngineBase.FACING, facing), Block.UPDATE_ALL);
        }
    }

    public InteractionResult attemptRotation() {
        Direction current = getCurrentFacing();
        for (int i = 0; i < 6; i++) {
            current = VanillaRotationHandlers.next(VanillaRotationHandlers.ROTATE_FACING, current);
            if (isFacingReceiver(current)) {
                if (getCurrentFacing() != current) {
                    setCurrentFacing(current);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.FAIL;
    }

    private boolean isFacingReceiver(Direction dir) {
        return getReceiverToPower(dir) != null;
    }

    protected final boolean canChain() {
        return getMaxChainLength() > 0;
    }

    /** @return The number of additional engines that this engine can send power through. */
    protected int getMaxChainLength() {
        return 2;
    }

    public void rotateIfInvalid() {
        if (isFacingReceiver(getCurrentFacing())) {
            return;
        }
        attemptRotation();
    }

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (!level.isClientSide) {
            isRedstonePowered = level.hasNeighborSignal(worldPosition);
            rotateIfInvalid();
        }
    }

    @Override
    public void onNeighbourBlockChanged(Block block, BlockPos neighbour) {
        super.onNeighbourBlockChanged(block, neighbour);
        isRedstonePowered = level.hasNeighborSignal(worldPosition);
    }

    // ##################
    //
    // Heat and power
    //
    // ##################

    public double getPowerLevel() {
        return power / (double) getMaxPower();
    }

    protected EnumPowerStage computePowerStage() {
        double heatLevel = getHeatLevel();
        if (heatLevel < 0.25f) return EnumPowerStage.BLUE;
        else if (heatLevel < 0.5f) return EnumPowerStage.GREEN;
        else if (heatLevel < 0.75f) return EnumPowerStage.YELLOW;
        else if (heatLevel < 0.85f) return EnumPowerStage.RED;
        else return EnumPowerStage.OVERHEAT;
    }

    /** @return The current stage. On the server this is recomputed, and the block state updated if it changed. */
    public final EnumPowerStage getPowerStage() {
        BlockState state = getBlockState();
        EnumPowerStage stage = state.getValue(BlockEngineBase.STAGE);
        if (level != null && !level.isClientSide) {
            EnumPowerStage newStage = computePowerStage();
            if (stage != newStage) {
                level.setBlock(worldPosition, state.setValue(BlockEngineBase.STAGE, newStage), Block.UPDATE_CLIENTS);
                stage = newStage;
            }
        }
        return stage;
    }

    public void updateHeatLevel() {
        heat = ((MAX_HEAT - MIN_HEAT) * getPowerLevel()) + MIN_HEAT;
    }

    public double getHeatLevel() {
        return (heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public double getIdealHeatLevel() {
        return heat / IDEAL_HEAT;
    }

    public double getHeat() {
        return heat;
    }

    public double getPistonSpeed() {
        switch (getPowerStage()) {
            case BLUE:
                return 0.02;
            case GREEN:
                return 0.04;
            case YELLOW:
                return 0.08;
            case RED:
                return 0.12;
            default:
                return 0;
        }
    }

    @Nonnull
    protected abstract IMjConnector createConnector();

    /** Block interactions happen before item ones, so engines with a GUI must let wrenches (to rotate the engine) and
     * pipes (to place them against the engine) through.
     *
     * @return True if the player is holding something that should be used on the engine rather than open its GUI. */
    protected static boolean isHoldingTool(Player player, InteractionHand hand) {
        Item item = player.getItemInHand(hand).getItem();
        return item instanceof IToolWrench || item instanceof IItemPipe;
    }

    @Override
    public void tick() {
        boolean overheat = getPowerStage() == EnumPowerStage.OVERHEAT;

        if (level.isClientSide) {
            lastProgress = progress;

            if (isPumping) {
                progress += getPistonSpeed();

                if (progress >= 1) {
                    progress = 0;
                }
            } else if (progress > 0) {
                progress -= 0.01f;
            }
            return;
        }

        lastPower = 0;

        if (!isRedstonePowered) {
            if (power > MjAPI.MJ) {
                power -= MjAPI.MJ;
            } else if (power > 0) {
                power = 0;
            }
        }

        updateHeatLevel();
        getPowerStage();
        engineUpdate();

        if (progressPart != 0) {
            progress += getPistonSpeed();

            if (progress > 0.5 && progressPart == 1) {
                progressPart = 2;
                sendPower();
            } else if (progress >= 1) {
                progress = 0;
                progressPart = 0;
            }
        } else if (isRedstonePowered && isActive()) {
            if (getPowerToExtract(false) > 0) {
                progressPart = 1;
                setPumping(true);
            } else {
                setPumping(false);
            }
        } else {
            setPumping(false);
        }

        if (!overheat) {
            burn();
        }

        markChunkDirty();
    }

    private long getPowerToExtract(boolean doExtract) {
        IMjReceiver receiver = getReceiverToPower(getCurrentFacing());
        if (receiver == null) {
            return 0;
        }
        // Pulsed power
        return extractPower(0, receiver.getPowerRequested(), doExtract);
    }

    private void sendPower() {
        IMjReceiver receiver = getReceiverToPower(getCurrentFacing());
        if (receiver != null) {
            long extracted = getPowerToExtract(true);
            if (extracted > 0) {
                long excess = receiver.receivePower(extracted, false);
                currentOutput = extracted - excess;
                // Give back whatever the receiver didn't accept
                power += excess;
            }
        }
    }

    protected void burn() {}

    protected void engineUpdate() {
        if (!isRedstonePowered) {
            if (power >= 1) {
                power -= 1;
            } else if (power < 1) {
                power = 0;
            }
        }
    }

    public boolean isActive() {
        return true;
    }

    protected final void setPumping(boolean isActive) {
        if (this.isPumping == isActive) {
            return;
        }
        this.isPumping = isActive;
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    /* STATE INFORMATION */
    public abstract boolean isBurning();

    public void addPower(long microJoules) {
        power += microJoules;
        lastPower += microJoules;

        if (power > getMaxPower()) {
            power = getMaxPower();
        }
    }

    public long extractPower(long min, long max, boolean doExtract) {
        if (power < min) {
            return 0;
        }

        long actualMax = Math.min(max, maxPowerExtracted());
        if (actualMax < min) {
            return 0;
        }

        long extracted = Math.min(power, actualMax);
        if (doExtract) {
            power -= extracted;
        }
        return extracted;
    }

    /** Follows chains of engines of the same type, all facing the same way, to the receiver at the end.
     *
     * @return The receiver that this engine would power when facing the given side, or null if there isn't one. */
    @Nullable
    public IMjReceiver getReceiverToPower(Direction side) {
        TileEngineBase engine = this;
        BlockEntity next = null;

        for (int len = 0; len <= getMaxChainLength(); len++) {
            next = engine.getNeighbourTile(side);

            if (next == null) {
                return null;
            }

            if (next instanceof TileEngineBase nextEngine) {
                if (next.getClass() != getClass() || nextEngine.getCurrentFacing() != side) {
                    return null;
                }
                engine = nextEngine;
            } else {
                break;
            }
        }

        if (next == null || next instanceof TileEngineBase) {
            return null;
        }

        IMjReceiver recv = next.getCapability(MjAPI.CAP_RECEIVER, side.getOpposite()).orElse(null);
        if (recv != null && recv.canConnect(mjConnector) && mjConnector.canConnect(recv)) {
            return recv;
        } else {
            return null;
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (facing == getCurrentFacing()) {
            LazyOptional<T> optional = mjCaps.getCapability(capability, facing);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    public abstract long getMaxPower();

    public long minPowerReceived() {
        return 2 * MjAPI.MJ;
    }

    public abstract long maxPowerReceived();

    public abstract long maxPowerExtracted();

    public abstract float explosionRange();

    public long getEnergyStored() {
        return power;
    }

    public long getLastPowerAdded() {
        return lastPower;
    }

    public abstract long getCurrentOutput();

    public boolean isEngineOn() {
        return isPumping;
    }

    /** @return How far the piston has moved, from 0 (down) to 1 (back down again), interpolated for rendering. */
    public float getProgressClient(float partialTicks) {
        float last = lastProgress;
        float now = progress;
        if (last > 0.5 && now < 0.5) {
            // we just returned
            now += 1;
        }
        float interp = last * (1 - partialTicks) + now * partialTicks;
        return interp % 1;
    }
}
