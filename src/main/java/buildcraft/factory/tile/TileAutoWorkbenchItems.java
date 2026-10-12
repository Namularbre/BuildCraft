/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;

import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;
import buildcraft.lib.tile.craft.WorkbenchCrafting;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerAutoCraftItems;

/** Crafts the recipe of its blueprint over and over, from the materials piped into it. It works slowly on its own,
 * and faster with power (a redstone engine is enough). */
public class TileAutoWorkbenchItems extends TileBC implements ITickable, IMjRedstoneReceiver {
    private static final int SIZE = 3;
    /** Power that the workbench makes for itself each tick: crafting takes 10 seconds without an engine. */
    private static final long POWER_GEN_PASSIVE = MjAPI.MJ / 5;
    public static final long POWER_REQUIRED = POWER_GEN_PASSIVE * 20 * 10;
    /** Power lost each tick while the workbench can't craft. */
    private static final long POWER_LOST = POWER_GEN_PASSIVE * 10;

    /** The recipe: copies of the items to use, never real items. */
    public final ItemStackHandler invBlueprint = new ItemStackHandler(SIZE * SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    /** Which item each material slot holds, so that the materials are spread to match the recipe. Worked out from
     * the blueprint. */
    private final ItemStack[] materialFilters = new ItemStack[SIZE * SIZE];
    public final ItemStackHandler invMaterials = new ItemStackHandler(SIZE * SIZE) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            ItemStack filter = materialFilters[slot];
            return filter.isEmpty() || ItemStack.isSameItemSameTags(filter, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    public final ItemStackHandler invResult = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    private final WorkbenchCrafting crafting =
        new WorkbenchCrafting(SIZE, SIZE, this, invBlueprint, invMaterials, invResult);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(this);

    /** The power stored until crafting can begin: it crafts once this reaches {@link #POWER_REQUIRED}. */
    private long powerStored;
    private long powerStoredLast;
    /** Client side: what the blueprint makes. */
    private ItemStack resultClient = ItemStack.EMPTY;

    public TileAutoWorkbenchItems(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.AUTO_WORKBENCH_ITEMS_TILE.get(), pos, state);
        Arrays.fill(materialFilters, ItemStack.EMPTY);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, new ExternalInventory(), EnumPipePart.VALUES);
        caps.addProvider(mjCaps);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }
        boolean recipeChanged = crafting.tick();
        if (crafting.canCraft()) {
            if (powerStored >= POWER_REQUIRED) {
                if (crafting.craft()) {
                    // Not 0 if it can go on, so that it doesn't look idle for the tick in between two crafts
                    powerStored = crafting.canCraft() ? 1 : 0;
                }
            } else {
                powerStored += POWER_GEN_PASSIVE;
            }
        } else {
            powerStored = Math.max(0, powerStored - POWER_LOST);
        }
        if (recipeChanged) {
            updateFilters(crafting.getAssumedResult().isEmpty() ? null : invBlueprint, materialFilters);
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    /** Spreads the material slots between the items of the blueprint, according to how many of each it needs (and
     * how many fit in a stack).
     *
     * @param blueprint The recipe, or null to accept anything in any slot. */
    public static void updateFilters(@Nullable IItemHandler blueprint, ItemStack[] filters) {
        Arrays.fill(filters, ItemStack.EMPTY);
        if (blueprint == null) {
            return;
        }
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> requirements = new ArrayList<>();
        for (int s = 0; s < blueprint.getSlots(); s++) {
            ItemStack stack = blueprint.getStackInSlot(s);
            if (stack.isEmpty()) {
                continue;
            }
            int index = -1;
            for (int i = 0; i < kinds.size(); i++) {
                if (ItemStack.isSameItemSameTags(kinds.get(i), stack)) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                kinds.add(stack);
                requirements.add(1);
            } else {
                requirements.set(index, requirements.get(index) + 1);
            }
        }
        if (kinds.isEmpty()) {
            return;
        }
        // Each item gets a slot, then every other slot goes to the item that would run out first
        int[] slotsPerKind = new int[kinds.size()];
        Arrays.fill(slotsPerKind, 1);
        for (int i = kinds.size(); i < filters.length; i++) {
            int smallest = Integer.MAX_VALUE;
            int smallestIndex = 0;
            for (int k = 0; k < kinds.size(); k++) {
                int crafts = kinds.get(k).getMaxStackSize() * slotsPerKind[k] / requirements.get(k);
                if (crafts < smallest) {
                    smallest = crafts;
                    smallestIndex = k;
                }
            }
            slotsPerKind[smallestIndex]++;
        }
        int slot = 0;
        for (int k = 0; k < kinds.size(); k++) {
            for (int i = 0; i < slotsPerKind[k]; i++) {
                filters[slot++] = kinds.get(k).copyWithCount(1);
            }
        }
    }

    public ItemStack getResultClient() {
        return resultClient;
    }

    /** @return How far (from 0 to 1) the current craft is. */
    public double getProgress(float partialTicks) {
        return (powerStoredLast + (powerStored - powerStoredLast) * partialTicks) / POWER_REQUIRED;
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int s = 0; s < invMaterials.getSlots(); s++) {
            drops.add(invMaterials.getStackInSlot(s));
        }
        drops.add(invResult.getStackInSlot(0));
        return drops;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerAutoCraftItems(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // IMjRedstoneReceiver

    @Override
    public boolean canConnect(@Nonnull IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return Math.max(0, POWER_REQUIRED - powerStored);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long taken = Math.min(getPowerRequested(), microJoules);
        if (!simulate) {
            powerStored += taken;
        }
        return microJoules - taken;
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("blueprint", invBlueprint.serializeNBT());
        nbt.put("materials", invMaterials.serializeNBT());
        nbt.put("result", invResult.serializeNBT());
        nbt.putLong("power", powerStored);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        invBlueprint.deserializeNBT(nbt.getCompound("blueprint"));
        invMaterials.deserializeNBT(nbt.getCompound("materials"));
        invResult.deserializeNBT(nbt.getCompound("result"));
        powerStored = nbt.getLong("power");
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_GUI_TICK) {
                buffer.writeLong(powerStored);
            } else if (id == NET_GUI_DATA) {
                buffer.writeItem(crafting.getAssumedResult());
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_GUI_TICK) {
                powerStoredLast = powerStored;
                powerStored = buffer.readLong();
                if (powerStored < powerStoredLast) {
                    // A craft just finished: don't go backwards
                    powerStoredLast = powerStored;
                }
            } else if (id == NET_GUI_DATA) {
                resultClient = buffer.readItem();
            }
        }
    }

    /** What other blocks see: they can put materials in (slots 0 to 8), and take the result out (slot 9). */
    private class ExternalInventory implements IItemHandler {
        private static final int RESULT = SIZE * SIZE;

        @Override
        public int getSlots() {
            return RESULT + 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == RESULT ? invResult.getStackInSlot(0) : invMaterials.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return slot == RESULT ? stack : invMaterials.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == RESULT ? invResult.extractItem(0, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot != RESULT && invMaterials.isItemValid(slot, stack);
        }
    }
}
