/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
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
import buildcraft.api.mj.MjAPI;

import buildcraft.lib.tile.craft.WorkbenchCrafting;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;

/** Crafts the recipe of its blueprint over and over with the power of lasers, from the materials piped into it. */
public class TileAdvancedCraftingTable extends TileLaserTableBase {
    public static final long POWER_REQ = 500 * MjAPI.MJ;

    /** The recipe: copies of the items to use, never real items. */
    public final ItemStackHandler invBlueprint = new ItemStackHandler(3 * 3) {
        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    public final ItemStackHandler invMaterials = new ItemStackHandler(5 * 3) {
        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    public final ItemStackHandler invResults = new ItemStackHandler(3 * 3) {
        @Override
        protected void onContentsChanged(int slot) {
            crafting.onInventoryChange(this);
            setChanged();
        }
    };
    private final WorkbenchCrafting crafting =
        new WorkbenchCrafting(3, 3, this, invBlueprint, invMaterials, invResults);
    /** Client side: what the blueprint makes. */
    private ItemStack resultClient = ItemStack.EMPTY;

    public TileAdvancedCraftingTable(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, new ExternalInventory(), EnumPipePart.VALUES);
    }

    @Override
    public long getTarget() {
        // The client doesn't know if it can craft: the GUI just shows the progress towards the next craft
        return level == null || level.isClientSide ? POWER_REQ : crafting.canCraft() ? POWER_REQ : 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            return;
        }
        boolean recipeChanged = crafting.tick();
        if (crafting.canCraft() && power >= POWER_REQ && crafting.craft()) {
            power -= POWER_REQ;
        }
        if (recipeChanged) {
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    public ItemStack getResultClient() {
        return resultClient;
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStackHandler inv : new ItemStackHandler[] { invMaterials, invResults }) {
            for (int s = 0; s < inv.getSlots(); s++) {
                drops.add(inv.getStackInSlot(s));
            }
        }
        return drops;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerAdvancedCraftingTable(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("blueprint", invBlueprint.serializeNBT());
        nbt.put("materials", invMaterials.serializeNBT());
        nbt.put("results", invResults.serializeNBT());
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        invBlueprint.deserializeNBT(nbt.getCompound("blueprint"));
        invMaterials.deserializeNBT(nbt.getCompound("materials"));
        invResults.deserializeNBT(nbt.getCompound("results"));
    }

    // Networking

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_GUI_DATA) {
            buffer.writeItem(crafting.getAssumedResult());
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_GUI_DATA) {
            resultClient = buffer.readItem();
        }
    }

    /** What other blocks see: they can put materials in (the first 15 slots), and take results out (the last 9). */
    private class ExternalInventory implements IItemHandler {
        @Override
        public int getSlots() {
            return invMaterials.getSlots() + invResults.getSlots();
        }

        private boolean isResult(int slot) {
            return slot >= invMaterials.getSlots();
        }

        private int resultSlot(int slot) {
            return slot - invMaterials.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return isResult(slot) ? invResults.getStackInSlot(resultSlot(slot)) : invMaterials.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return isResult(slot) ? stack : invMaterials.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isResult(slot) ? invResults.extractItem(resultSlot(slot), amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return !isResult(slot);
        }
    }
}
