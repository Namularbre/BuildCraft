/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase;

import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.container.ContainerEngineStone;

/** The Stirling engine: burns furnace fuel, aiming to keep its internal power at 3/8 of its maximum. */
public class TileEngineStone extends TileEngineBase {
    private static final long MAX_OUTPUT = MjAPI.MJ;
    private static final long MIN_OUTPUT = MAX_OUTPUT / 3;
    private static final long E_LIMIT = (MAX_OUTPUT - MIN_OUTPUT) * 20;

    /** The fuel slot. Accepts anything that burns, plus the container items left over by burnt fuel (like buckets). */
    public final ItemStackHandler invFuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isForceInserting || getItemBurnTime(stack) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (isForceInserting && getStackInSlot(slot).isEmpty()) {
                isForceInserting = false;
            }
            setChanged();
        }
    };

    /** The burn times, for the menu. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? burnTime : totalBurnTime;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                burnTime = value;
            } else {
                totalBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    int burnTime = 0;
    int totalBurnTime = 0;
    long esum = 0;

    private boolean isForceInserting = false;

    public TileEngineStone(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_STONE_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, invFuel, EnumPipePart.VALUES);
    }

    private static int getItemBurnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    // Saving

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        invFuel.deserializeNBT(nbt.getCompound("items"));
        burnTime = nbt.getInt("burnTime");
        totalBurnTime = nbt.getInt("totalBurnTime");
        esum = nbt.getLong("esum");
        isForceInserting = nbt.getBoolean("isForceInserting");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("items", invFuel.serializeNBT());
        nbt.putInt("burnTime", burnTime);
        nbt.putInt("totalBurnTime", totalBurnTime);
        nbt.putLong("esum", esum);
        nbt.putBoolean("isForceInserting", isForceInserting);
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        return List.of(invFuel.getStackInSlot(0));
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerEngineStone(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Engine overrides

    @Nonnull
    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return burnTime > 0;
    }

    @Override
    protected void engineUpdate() {
        super.engineUpdate();
        if (burnTime > 0) {
            burnTime--;
            if (getPowerStage() != EnumPowerStage.OVERHEAT) {
                long output = getCurrentOutput();
                currentOutput = output;
                addPower(output);
            }
        }
    }

    @Override
    public void burn() {
        if (burnTime == 0 && isRedstonePowered) {
            ItemStack fuelStack = invFuel.getStackInSlot(0);
            burnTime = totalBurnTime = getItemBurnTime(fuelStack);

            if (burnTime > 0) {
                ItemStack container = fuelStack.getCraftingRemainingItem();
                invFuel.extractItem(0, 1, false);
                if (!container.isEmpty()) {
                    if (invFuel.getStackInSlot(0).isEmpty()) {
                        // Let the container (for example an empty bucket) into the slot, even though it doesn't burn
                        isForceInserting = true;
                        invFuel.setStackInSlot(0, container);
                    } else {
                        // Nowhere to put it
                        Containers.dropItemStack(
                            level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                            container
                        );
                    }
                }
            }
        }
    }

    @Override
    public long maxPowerReceived() {
        return 200 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 100 * MjAPI.MJ;
    }

    @Override
    public long getMaxPower() {
        return 1000 * MjAPI.MJ;
    }

    @Override
    public float explosionRange() {
        return 2;
    }

    /** A PI controller that aims to keep the stored power at 3/8 of the maximum. */
    @Override
    public long getCurrentOutput() {
        long e = 3 * getMaxPower() / 8 - power;
        esum = clamp(esum + e, -E_LIMIT, E_LIMIT);
        return clamp(e + esum / 20, MIN_OUTPUT, MAX_OUTPUT);
    }

    private static long clamp(long val, long min, long max) {
        return Math.max(min, Math.min(max, val));
    }
}
