/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.transport.IInjectable;

import buildcraft.lib.inventory.InsertOnlyItemHandler;
import buildcraft.lib.inventory.ItemTransactorHelper;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.container.ContainerChute;

/** Picks up the items in front of its opening (using a little MJ, or for free when it faces up), and sends every item
 * it holds to the inventories and pipes on its other sides. */
public class TileChute extends TileBC implements ITickable {
    /** The most items picked up at once. */
    private static final int PICKUP_MAX = 3;
    /** The power needed to pick items up. */
    public static final long PICKUP_POWER = MjAPI.MJ / 10;
    /** Chutes facing up get this much power each tick for free: items fall in. */
    private static final long GRAVITY_POWER = MjAPI.MJ / 1000;

    public final ItemStackHandler inv = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final MjBattery battery = new MjBattery(MjAPI.MJ);
    private final MjCapabilityHelper mjCaps = new MjCapabilityHelper(new MjBatteryReceiver(battery));
    private long progress = 0;

    public TileChute(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.CHUTE_TILE.get(), pos, state);
        caps.addCapabilityInstance(
            ForgeCapabilities.ITEM_HANDLER, new InsertOnlyItemHandler(inv), EnumPipePart.VALUES
        );
        caps.addProvider(mjCaps);
    }

    /** @return True if the block at the given position (on the given side of a chute) can take items from it. */
    public static boolean hasInventoryAtPosition(BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity tile = level.getBlockEntity(pos);
        return ItemTransactorHelper.getItemHandler(tile, side.getOpposite()) != null
            || ItemTransactorHelper.getInjectable(tile, side.getOpposite()) != null;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        mjCaps.invalidate();
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockChute.FACING) ? state.getValue(BlockChute.FACING) : Direction.UP;
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }
        battery.tick(level, worldPosition);
        Direction facing = getFacing();

        if (facing == Direction.UP) {
            progress += GRAVITY_POWER;
        }
        progress += battery.extractPower(0, PICKUP_POWER - progress);
        if (progress >= PICKUP_POWER) {
            progress = 0;
            pickupItems(facing);
        }

        putInNearInventories(facing);
    }

    private void pickupItems(Direction facing) {
        AABB area = extrudeFace(worldPosition, facing, 0.25);
        int count = PICKUP_MAX;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, EntitySelector.ENTITY_STILL_ALIVE)) {
            ItemStack stack = entity.getItem();
            ItemStack toInsert = stack.copyWithCount(Math.min(count, stack.getCount()));
            ItemStack leftOver = ItemHandlerHelper.insertItemStacked(inv, toInsert, false);
            int moved = toInsert.getCount() - leftOver.getCount();
            if (moved > 0) {
                ItemStack remaining = stack.copy();
                remaining.shrink(moved);
                if (remaining.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(remaining);
                }
                count -= moved;
            }
            if (count <= 0) {
                return;
            }
        }
    }

    /** @return A box of the given thickness just outside of the given face of the block. */
    private static AABB extrudeFace(BlockPos pos, Direction face, double thickness) {
        AABB box = new AABB(pos);
        return switch (face) {
            case DOWN -> new AABB(box.minX, box.minY - thickness, box.minZ, box.maxX, box.minY, box.maxZ);
            case UP -> new AABB(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + thickness, box.maxZ);
            case NORTH -> new AABB(box.minX, box.minY, box.minZ - thickness, box.maxX, box.maxY, box.minZ);
            case SOUTH -> new AABB(box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ + thickness);
            case WEST -> new AABB(box.minX - thickness, box.minY, box.minZ, box.minX, box.maxY, box.maxZ);
            case EAST -> new AABB(box.maxX, box.minY, box.minZ, box.maxX + thickness, box.maxY, box.maxZ);
        };
    }

    /** Sends one item to each block (or non-living entity, like a minecart with a chest) on every side but the
     * opening. */
    private void putInNearInventories(Direction facing) {
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.remove(facing);
        Collections.shuffle(sides);
        for (Direction side : sides) {
            BlockPos neighbourPos = worldPosition.relative(side);
            BlockEntity tile = level.getBlockEntity(neighbourPos);
            IInjectable injectable = ItemTransactorHelper.getInjectable(tile, side.getOpposite());
            if (injectable != null && injectable.canInjectItems(side.getOpposite())) {
                moveOne(stack -> injectable.injectItem(stack, false, side.getOpposite(), null, 0),
                    stack -> injectable.injectItem(stack, true, side.getOpposite(), null, 0));
            } else {
                moveOne(ItemTransactorHelper.getItemHandler(tile, side.getOpposite()));
            }
        }
        for (Direction side : sides) {
            for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(worldPosition.relative(side)),
                entity -> !(entity instanceof LivingEntity) && entity.isAlive())) {
                moveOne(entity.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite()).orElse(null));
            }
        }
    }

    private void moveOne(@Nullable IItemHandler target) {
        if (target != null) {
            moveOne(stack -> ItemHandlerHelper.insertItemStacked(target, stack, true),
                stack -> ItemHandlerHelper.insertItemStacked(target, stack, false));
        }
    }

    /** Moves the first item that the target accepts.
     *
     * @param simulate Returns what wouldn't be accepted, without changing anything.
     * @param insert Returns what wasn't accepted. */
    private void moveOne(UnaryOperator<ItemStack> simulate,
        UnaryOperator<ItemStack> insert) {
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.extractItem(slot, 1, true);
            if (!stack.isEmpty() && simulate.apply(stack).isEmpty()) {
                ItemStack leftOver = insert.apply(inv.extractItem(slot, 1, false));
                if (!leftOver.isEmpty()) {
                    inv.insertItem(slot, leftOver, false);
                }
                return;
            }
        }
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            drops.add(inv.getStackInSlot(slot));
        }
        return drops;
    }

    // Interaction

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerChute(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("items", inv.serializeNBT());
        nbt.putLong("progress", progress);
        nbt.put("battery", battery.serializeNBT());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        inv.deserializeNBT(nbt.getCompound("items"));
        progress = nbt.getLong("progress");
        battery.deserializeNBT(nbt.getCompound("battery"));
    }
}
