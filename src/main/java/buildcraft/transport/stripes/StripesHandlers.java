/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.EnumHandlerPriority;
import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IStripesHandlerItem;
import buildcraft.api.transport.IStripesRegistry;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;

/** What stripes pipes do with the items that reach their end. */
public final class StripesHandlers {
    private StripesHandlers() {}

    public static void register(IStripesRegistry registry) {
        registry.addHandler(StripesHandlers::plant);
        registry.addHandler(StripesHandlers::shear);
        registry.addHandler(StripesHandlers::extendPipe);
        registry.addHandler(StripesHandlers::interactWithEntity, EnumHandlerPriority.LOW);
        registry.addHandler(StripesHandlers::till);
        registry.addHandler(StripesHandlers::dispense, EnumHandlerPriority.LOW);
        registry.addHandler(StripesHandlers::placeBlock, EnumHandlerPriority.LOW);
    }

    private static BlockHitResult hit(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
    }

    /** Plants seeds (and saplings) in front of the pipe, or on the block in front of it. */
    private static boolean plant(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof IPlantable plant)) {
            return false;
        }
        BlockPos front = pos.relative(direction);
        for (BlockPos soil : new BlockPos[] { front.below(), front }) {
            BlockPos plantPos = soil.above();
            BlockState soilState = level.getBlockState(soil);
            if (level.isEmptyBlock(plantPos) && soilState.canSustainPlant(level, soil, Direction.UP, plant)) {
                level.setBlock(plantPos, plant.getPlant(level, plantPos), Block.UPDATE_ALL);
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Shears the block in front of the pipe, if shears make it drop something else than without them (like
     * leaves, grass and vines). */
    private static boolean shear(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        if (!(stack.getItem() instanceof ShearsItem) || !(level instanceof ServerLevel server)) {
            return false;
        }
        BlockPos front = pos.relative(direction);
        BlockState state = level.getBlockState(front);
        if (state.isAir() || state.getDestroySpeed(level, front) < 0) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(front);
        Item self = state.getBlock().asItem();
        List<ItemStack> sheared = Block.getDrops(state, server, front, tile, player, stack);
        List<ItemStack> normal = Block.getDrops(state, server, front, tile, player, ItemStack.EMPTY);
        boolean onlyWithShears = sheared.stream().anyMatch(s -> s.is(self))
            && normal.stream().noneMatch(s -> s.is(self));
        if (!onlyWithShears) {
            return false;
        }
        stack.hurtAndBreak(1, player, p -> {});
        level.setBlock(front, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack drop : sheared) {
            activator.sendItem(drop, direction);
        }
        return true;
    }

    /** Item pipes make the stripes pipe move forwards (or backwards, for void pipes). */
    private static boolean extendPipe(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        if (!(stack.getItem() instanceof IItemPipe itemPipe)) {
            return false;
        }
        PipeDefinition definition = itemPipe.getDefinition();
        if (definition.flowType != PipeApi.flowItems) {
            return false;
        }
        if (PipeApi.extensionManager.requestPipeExtension(level, pos, direction, activator, stack.copy())) {
            // The pipe extension manager sends the items back later
            player.getInventory().clearContent();
            return true;
        }
        return false;
    }

    /** Uses the item on a living entity in front of the pipe (like shears on sheep, or wheat on cows). */
    private static boolean interactWithEntity(Level level, BlockPos pos, Direction direction, ItemStack stack,
        Player player, IStripesActivator activator) {
        List<LivingEntity> entities = new ArrayList<>(
            level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.relative(direction)))
        );
        Collections.shuffle(entities);
        for (LivingEntity entity : entities) {
            if (player.interactOn(entity, InteractionHand.MAIN_HAND).consumesAction()) {
                return true;
            }
        }
        return false;
    }

    /** Hoes till the block in front of the pipe (or the one under it). */
    private static boolean till(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        if (!(stack.getItem() instanceof HoeItem)) {
            return false;
        }
        BlockPos front = pos.relative(direction);
        if (stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(front, Direction.UP)))
            .consumesAction()) {
            return true;
        }
        return direction != Direction.UP
            && stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(front.below(), Direction.UP)))
                .consumesAction();
    }

    /** Places the block in front of the pipe. */
    private static boolean placeBlock(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        BlockPos front = pos.relative(direction);
        if (!level.isEmptyBlock(front)) {
            return false;
        }
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack,
            hit(front, direction.getOpposite()));
        return blockItem.place(context).consumesAction();
    }

    // Dispenser behaviours (like buckets, arrows, eggs and spawn eggs)

    @Nullable
    private static Map<Item, DispenseItemBehavior> dispenserBehaviours;
    private static boolean dispenserBehavioursFailed = false;

    @Nullable
    @SuppressWarnings("unchecked")
    private static Map<Item, DispenseItemBehavior> getDispenserBehaviours() {
        if (dispenserBehaviours == null && !dispenserBehavioursFailed) {
            try {
                dispenserBehaviours = ObfuscationReflectionHelper.getPrivateValue(DispenserBlock.class, null,
                    "f_52661_");
            } catch (RuntimeException e) {
                dispenserBehavioursFailed = true;
                BCLog.logger.warn("[transport.stripes] Couldn't find the dispenser behaviours", e);
            }
        }
        return dispenserBehaviours;
    }

    /** Uses the item like a dispenser facing the same way as the pipe would. */
    private static boolean dispense(Level level, BlockPos pos, Direction direction, ItemStack stack, Player player,
        IStripesActivator activator) {
        Map<Item, DispenseItemBehavior> behaviours = getDispenserBehaviours();
        if (behaviours == null || !behaviours.containsKey(stack.getItem()) || !(level instanceof ServerLevel server)) {
            return false;
        }
        try {
            ItemStack output = behaviours.get(stack.getItem()).dispense(new Source(server, pos, direction),
                stack.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, output);
        } catch (ClassCastException e) {
            // Some behaviours expect a real dispenser: the item was used anyway
            BCLog.logger.debug("[transport.stripes] {} needs a real dispenser", stack.getItem());
        }
        return true;
    }

    /** A dispenser that faces the same way as the stripes pipe. */
    private record Source(ServerLevel level, BlockPos pos, Direction side) implements BlockSource {
        @Override
        public double x() {
            return pos.getX() + 0.5;
        }

        @Override
        public double y() {
            return pos.getY() + 0.5;
        }

        @Override
        public double z() {
            return pos.getZ() + 0.5;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public BlockState getBlockState() {
            return Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, side);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> T getEntity() {
            return (T) level.getBlockEntity(pos);
        }

        @Override
        public ServerLevel getLevel() {
            return level;
        }
    }
}
