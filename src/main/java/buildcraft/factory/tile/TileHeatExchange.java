/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager.ICoolableRecipe;
import buildcraft.api.recipes.IRefineryRecipeManager.IHeatableRecipe;
import buildcraft.api.recipes.IRefineryRecipeManager;

import buildcraft.lib.fluid.RestrictedFluidHandler;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockHeatExchange.EnumExchangePart;
import buildcraft.factory.block.BlockHeatExchange;

/** One block of a heat exchanger. The first block of a row (the start, clockwise of the facing) heats the fluid that
 * goes in from below, and the last block (the end) cools the fluid that goes in through its outer side. The heated
 * fluid comes out of the top of the end, and the cooled fluid out of the outer side of the start. */
public class TileHeatExchange extends TileBC implements ITickable {
    public static final IdAllocator IDS = TileBC.IDS.makeChild("heat_exchange");
    public static final int NET_STATE = IDS.allocId("STATE");

    public static final int CAPACITY = 2 * FluidType.BUCKET_VOLUME;
    /** How many mB are exchanged each tick, for 1, 2 or 3 middle blocks. */
    private static final int[] FLUID_MULT = { 5, 10, 20 };
    /** How many ticks the fluids take to flow through the exchanger, before it starts exchanging. */
    public static final int PROGRESS_MAX = 120;
    /** Minimum number of ticks between two updates of the tanks sent to clients. */
    private static final int NET_UPDATE_DELAY = 4;

    public enum EnumProgressState {
        /** Progress is at 0, not moving. */
        OFF,
        /** Progress is increasing from 0 to max. */
        PREPARING,
        /** Progress stays at max: fluids are being exchanged. */
        RUNNING,
        /** Progress is decreasing from max to 0. */
        STOPPING
    }

    /** The tanks of the start or the end of a heat exchanger. They move with the part when it moves to another
     * block. */
    public static final class Section {
        public final boolean isStart;
        public final FluidTank tankInput, tankOutput;
        private TileHeatExchange tile;

        Section(TileHeatExchange tile, boolean isStart) {
            this.tile = tile;
            this.isStart = isStart;
            // The start heats its input, the end cools its input
            Predicate<FluidStack> canExchange = isStart ? TileHeatExchange::isHeatable : TileHeatExchange::isCoolable;
            tankInput = new FluidTank(CAPACITY, canExchange) {
                @Override
                protected void onContentsChanged() {
                    Section.this.tile.tanksChanged();
                }
            };
            tankOutput = new FluidTank(CAPACITY) {
                @Override
                protected void onContentsChanged() {
                    Section.this.tile.tanksChanged();
                }
            };
        }

        CompoundTag save() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("start", isStart);
            nbt.put("input", tankInput.writeToNBT(new CompoundTag()));
            nbt.put("output", tankOutput.writeToNBT(new CompoundTag()));
            return nbt;
        }

        static Section load(TileHeatExchange tile, CompoundTag nbt) {
            Section section = new Section(tile, nbt.getBoolean("start"));
            section.tankInput.readFromNBT(nbt.getCompound("input"));
            section.tankOutput.readFromNBT(nbt.getCompound("output"));
            return section;
        }
    }

    @Nullable
    private Section section;
    /** Only used by the start: how many blocks are between it and the end. */
    private int middleCount;
    private boolean checkNeighbours = true;

    private int progress, progressLast;
    private EnumProgressState progressState = EnumProgressState.OFF;
    private EnumProgressState lastSentState = EnumProgressState.OFF;

    private boolean tanksChanged = false;
    private long nextNetworkUpdate;

    public TileHeatExchange(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.HEAT_EXCHANGE_TILE.get(), pos, state);
        caps.addCapability(ForgeCapabilities.FLUID_HANDLER, this::getFluidHandler, EnumPipePart.VALUES);
    }

    private static boolean isHeatable(FluidStack fluid) {
        return BuildcraftRecipeRegistry.refineryRecipes.getHeatableRegistry().getRecipeForInput(fluid) != null;
    }

    private static boolean isCoolable(FluidStack fluid) {
        return BuildcraftRecipeRegistry.refineryRecipes.getCoolableRegistry().getRecipeForInput(fluid) != null;
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    @Nullable
    private IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (section == null || side == null) {
            return null;
        }
        Direction facing = getFacing();
        if (section.isStart) {
            if (side == Direction.DOWN) {
                return RestrictedFluidHandler.fillOnly(section.tankInput);
            } else if (side == facing.getClockWise()) {
                return RestrictedFluidHandler.drainOnly(section.tankOutput);
            }
        } else {
            if (side == Direction.UP) {
                return RestrictedFluidHandler.drainOnly(section.tankOutput);
            } else if (side == facing.getCounterClockWise()) {
                return RestrictedFluidHandler.fillOnly(section.tankInput);
            }
        }
        return null;
    }

    public Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockHeatExchange.FACING) ? state.getValue(BlockHeatExchange.FACING) : Direction.NORTH;
    }

    @Nullable
    public Section getSection() {
        return section;
    }

    public boolean isStart() {
        return section != null && section.isStart;
    }

    public boolean isEnd() {
        return section != null && !section.isStart;
    }

    public int getMiddleCount() {
        return middleCount;
    }

    /** @return The end of this heat exchanger, if this is its start. */
    @Nullable
    public TileHeatExchange getEnd() {
        if (!isStart() || middleCount < 1) {
            return null;
        }
        BlockPos endPos = worldPosition.relative(getFacing().getCounterClockWise(), middleCount + 1);
        return level.getBlockEntity(endPos) instanceof TileHeatExchange end && end.isEnd() ? end : null;
    }

    public EnumProgressState getProgressState() {
        return progressState;
    }

    /** @return How far (from 0 to 1) the fluids have gone through the exchanger. */
    public double getProgress(float partialTicks) {
        return (progressLast + (progress - progressLast) * partialTicks) / PROGRESS_MAX;
    }

    private void tanksChanged() {
        tanksChanged = true;
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        checkNeighbours = true;
    }

    @Override
    public void onNeighbourBlockChanged(Block block, BlockPos neighbour) {
        // Heat exchangers can only be next to each other horizontally
        if (neighbour.getY() == worldPosition.getY()) {
            checkNeighbours = true;
        }
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            if (isStart()) {
                updateProgress();
                spawnParticles();
            }
            return;
        }
        if (checkNeighbours) {
            checkNeighbours = false;
            rebuildStructure();
        }
        if (isStart()) {
            updateProgress();
            if (getEnd() != null) {
                exchange();
            } else if (progressState != EnumProgressState.OFF) {
                progressState = EnumProgressState.STOPPING;
            }
            output();
            if (progressState != lastSentState) {
                lastSentState = progressState;
                sendNetworkUpdate(NET_STATE);
            }
        } else if (isEnd()) {
            output();
        }
        long now = level.getGameTime();
        if (tanksChanged && now >= nextNetworkUpdate) {
            tanksChanged = false;
            nextNetworkUpdate = now + NET_UPDATE_DELAY;
            sendNetworkUpdate(NET_RENDER_DATA);
        }
    }

    // Structure

    /** @return Every heat exchanger in a row with this one facing the same way, from the start side to the end side
     *         (up to 5 on either side of this one). */
    private Deque<TileHeatExchange> findAdjacentExchangers() {
        Direction facing = getFacing();
        Deque<TileHeatExchange> exchangers = new ArrayDeque<>();
        exchangers.add(this);
        for (int i = 1; i < 6; i++) {
            TileHeatExchange other = getExchanger(worldPosition.relative(facing.getClockWise(), i), facing);
            if (other == null) {
                break;
            }
            exchangers.addFirst(other);
        }
        for (int i = 1; i < 6; i++) {
            TileHeatExchange other = getExchanger(worldPosition.relative(facing.getCounterClockWise(), i), facing);
            if (other == null) {
                break;
            }
            exchangers.addLast(other);
        }
        return exchangers;
    }

    @Nullable
    private TileHeatExchange getExchanger(BlockPos pos, Direction facing) {
        BlockEntity tile = level.getBlockEntity(pos);
        return tile instanceof TileHeatExchange exchange && exchange.getFacing() == facing ? exchange : null;
    }

    /** Makes the first block of the row the start, and the last block the end, keeping their tanks. Rows of less than
     * 3 or more than 5 blocks don't work. */
    private void rebuildStructure() {
        Deque<TileHeatExchange> exchangers = findAdjacentExchangers();
        if (exchangers.size() < 3 || exchangers.size() > 5) {
            for (TileHeatExchange exchange : exchangers) {
                exchange.setSection(null);
            }
            return;
        }
        Section start = null;
        Section end = null;
        for (TileHeatExchange exchange : exchangers) {
            exchange.checkNeighbours = false;
            if (exchange.section != null) {
                if (exchange.section.isStart && start == null) {
                    start = exchange.section;
                } else if (!exchange.section.isStart && end == null) {
                    end = exchange.section;
                }
            }
        }
        TileHeatExchange first = exchangers.getFirst();
        TileHeatExchange last = exchangers.getLast();
        for (TileHeatExchange exchange : exchangers) {
            if (exchange != first && exchange != last) {
                exchange.setSection(null);
            }
        }
        first.middleCount = exchangers.size() - 2;
        first.setSection(start != null ? start : new Section(first, true));
        last.setSection(end != null ? end : new Section(last, false));
        first.sendNetworkUpdate(NET_RENDER_DATA);
    }

    private void setSection(@Nullable Section newSection) {
        if (section == newSection) {
            return;
        }
        section = newSection;
        if (newSection != null) {
            newSection.tile = this;
        } else {
            middleCount = 0;
            progress = progressLast = 0;
            progressState = EnumProgressState.OFF;
        }
        EnumExchangePart part = newSection == null ? EnumExchangePart.MIDDLE
            : newSection.isStart ? EnumExchangePart.START : EnumExchangePart.END;
        BlockState state = getBlockState();
        if (state.hasProperty(BlockHeatExchange.PART) && state.getValue(BlockHeatExchange.PART) != part) {
            level.setBlock(worldPosition, state.setValue(BlockHeatExchange.PART, part), Block.UPDATE_ALL);
        }
        caps.invalidate();
        setChanged();
        sendNetworkUpdate(NET_RENDER_DATA);
    }

    /** Turns a lone heat exchanger by a quarter, or a whole row around, swapping its start and end (and their
     * tanks).
     *
     * @return True if anything turned. */
    public boolean rotate() {
        Direction facing = getFacing();
        Deque<TileHeatExchange> exchangers = findAdjacentExchangers();
        if (exchangers.size() == 1) {
            setFacing(this, facing.getClockWise());
            checkNeighbours = true;
            return true;
        }
        Section start = null;
        Section end = null;
        for (TileHeatExchange exchange : exchangers) {
            if (exchange.isStart()) {
                start = exchange.section;
            } else if (exchange.isEnd()) {
                end = exchange.section;
            }
            exchange.section = null;
        }
        for (TileHeatExchange exchange : exchangers) {
            setFacing(exchange, facing.getOpposite());
            exchange.checkNeighbours = true;
        }
        // The old end side is now the start side
        exchangers.getLast().middleCount = exchangers.size() - 2;
        if (start != null) {
            exchangers.getLast().setSection(start);
        }
        if (end != null) {
            exchangers.getFirst().setSection(end);
        }
        return true;
    }

    private static void setFacing(TileHeatExchange exchange, Direction facing) {
        BlockState state = exchange.getBlockState().setValue(BlockHeatExchange.FACING, facing);
        state = BlockHeatExchange.updateConnections(state, exchange.level, exchange.worldPosition);
        exchange.level.setBlock(exchange.worldPosition, state, Block.UPDATE_ALL);
    }

    // Exchanging

    private void updateProgress() {
        progressLast = progress;
        switch (progressState) {
            case STOPPING -> {
                progress--;
                if (progress <= 0) {
                    progress = 0;
                    progressState = EnumProgressState.OFF;
                }
            }
            case PREPARING, RUNNING -> {
                progress++;
                if (progress >= PROGRESS_MAX) {
                    progress = PROGRESS_MAX;
                    progressState = EnumProgressState.RUNNING;
                }
            }
            default -> {}
        }
    }

    private void exchange() {
        TileHeatExchange endTile = getEnd();
        Section start = section;
        Section end = endTile.section;
        FluidTank coolIn = end.tankInput;
        FluidTank coolOut = start.tankOutput;
        FluidTank heatIn = start.tankInput;
        FluidTank heatOut = end.tankOutput;
        IRefineryRecipeManager registry = BuildcraftRecipeRegistry.refineryRecipes;
        ICoolableRecipe coolRecipe = registry.getCoolableRegistry().getRecipeForInput(coolIn.getFluid());
        IHeatableRecipe heatRecipe = registry.getHeatableRegistry().getRecipeForInput(heatIn.getFluid());
        // The fluid being cooled has to be hotter than the fluid being heated
        if (coolRecipe == null || heatRecipe == null || coolRecipe.heatFrom() <= heatRecipe.heatFrom()) {
            stop();
            return;
        }

        // The most that can be exchanged this tick: limited by the size, and by what the tanks hold
        int max = FLUID_MULT[Math.min(middleCount, FLUID_MULT.length) - 1];
        int amount = Math.min(
            Math.min(fillable(coolOut, coolRecipe.out(), max), fillable(heatOut, heatRecipe.out(), max)),
            Math.min(drainable(coolIn, coolRecipe.in(), max), drainable(heatIn, heatRecipe.in(), max))
        );
        if (amount <= 0) {
            stop();
            return;
        }
        if (progressState == EnumProgressState.OFF || progressState == EnumProgressState.STOPPING) {
            progressState = EnumProgressState.PREPARING;
        } else if (progressState == EnumProgressState.RUNNING) {
            fill(coolOut, coolRecipe.out(), amount);
            coolIn.drain(new FluidStack(coolRecipe.in(), amount), FluidAction.EXECUTE);
            fill(heatOut, heatRecipe.out(), amount);
            heatIn.drain(new FluidStack(heatRecipe.in(), amount), FluidAction.EXECUTE);
        }
    }

    private void stop() {
        if (progressState != EnumProgressState.OFF) {
            progressState = EnumProgressState.STOPPING;
        }
    }

    /** @param out The fluid made, or null if the fluid is used up (like water and lava). */
    private static int fillable(FluidTank tank, @Nullable FluidStack out, int max) {
        return out == null ? max : tank.fill(new FluidStack(out, max), FluidAction.SIMULATE);
    }

    private static int drainable(FluidTank tank, FluidStack in, int max) {
        return tank.drain(new FluidStack(in, max), FluidAction.SIMULATE).getAmount();
    }

    private static void fill(FluidTank tank, @Nullable FluidStack out, int amount) {
        if (out != null) {
            tank.fill(new FluidStack(out, amount), FluidAction.EXECUTE);
        }
    }

    /** The start sends the cooled fluid to the block on its outer side, and the end sends the heated fluid up. */
    private void output() {
        Direction side = isStart() ? getFacing().getClockWise() : Direction.UP;
        BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(side));
        if (neighbour == null) {
            return;
        }
        neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).ifPresent(
            target -> FluidUtil.tryFluidTransfer(target, section.tankOutput, FluidType.BUCKET_VOLUME, true)
        );
    }

    private void spawnParticles() {
        TileHeatExchange end = getEnd();
        if (progressState != EnumProgressState.RUNNING || end == null) {
            return;
        }
        // Cooling lava gives smoke where the cooled fluid comes out, heating water gives steam
        if (end.section.tankInput.getFluid().getFluid().isSame(Fluids.LAVA)) {
            spewForth(worldPosition, getFacing().getClockWise(), ParticleTypes.LARGE_SMOKE);
        }
        if (section.tankInput.getFluid().getFluid().isSame(Fluids.WATER)) {
            spewForth(end.worldPosition, Direction.UP, ParticleTypes.CLOUD);
        }
    }

    private void spewForth(BlockPos from, Direction dir, ParticleOptions particle) {
        Vec3 dirVec = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 pos = Vec3.atCenterOf(from).add(dirVec);
        Vec3 motion = dirVec.scale(0.4);
        int count = 2;
        for (int i = 0; i < count; i++) {
            double dx = motion.x + (level.random.nextDouble() - 0.5) * 0.1;
            double dy = motion.y + (level.random.nextDouble() - 0.5) * 0.1;
            double dz = motion.z + (level.random.nextDouble() - 0.5) * 0.1;
            level.addParticle(particle, pos.x, pos.y, pos.z, dx, dy, dz);
        }
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (section != null) {
            nbt.put("section", section.save());
        }
        nbt.putInt("middleCount", middleCount);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        section = nbt.contains("section") ? Section.load(this, nbt.getCompound("section")) : null;
        middleCount = nbt.getInt("middleCount");
        checkNeighbours = true;
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                buffer.writeBoolean(section != null);
                if (section != null) {
                    buffer.writeBoolean(section.isStart);
                    buffer.writeFluidStack(section.tankInput.getFluid());
                    buffer.writeFluidStack(section.tankOutput.getFluid());
                }
                buffer.writeByte(middleCount);
                writePayload(NET_STATE, buffer, side);
            } else if (id == NET_STATE) {
                buffer.writeEnum(progressState);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                if (buffer.readBoolean()) {
                    boolean isStart = buffer.readBoolean();
                    if (section == null || section.isStart != isStart) {
                        section = new Section(this, isStart);
                    }
                    section.tankInput.setFluid(buffer.readFluidStack());
                    section.tankOutput.setFluid(buffer.readFluidStack());
                } else {
                    section = null;
                }
                middleCount = buffer.readUnsignedByte();
                readPayload(NET_STATE, buffer, side, sender);
            } else if (id == NET_STATE) {
                progressState = buffer.readEnum(EnumProgressState.class);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        // The start draws the fluids flowing through the whole row
        return isStart() ? new AABB(worldPosition).inflate(6) : super.getRenderBoundingBox();
    }
}
