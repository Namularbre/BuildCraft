/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Thrown into water, gel spreads to the water source blocks around it, then sets into a block that can be mined
 * (for gel, which turns back into water in a bucket). */
public class BlockWaterGel extends Block {
    public enum GelStage implements StringRepresentable {
        SPREAD_0(0.3f, true, 3),
        SPREAD_1(0.4f, true, 3),
        SPREAD_2(0.6f, true, 3),
        SPREAD_3(0.8f, true, 3),
        GELLING_0(1.0f, false, 0.6f),
        GELLING_1(1.2f, false, 0.6f),
        GEL(1.5f, false, 0.1f);

        public final SoundType soundType;
        public final boolean spreading;
        public final float hardness;
        private final String name = name().toLowerCase(Locale.ROOT);

        GelStage(float pitch, boolean spreading, float hardness) {
            this.soundType = new SoundType(
                SoundType.SLIME_BLOCK.getVolume(), pitch, SoundEvents.SLIME_BLOCK_BREAK, SoundEvents.SLIME_BLOCK_STEP,
                SoundEvents.SLIME_BLOCK_PLACE, SoundEvents.SLIME_BLOCK_HIT, SoundEvents.SLIME_BLOCK_FALL
            );
            this.spreading = spreading;
            this.hardness = hardness;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public GelStage next() {
            return this == GEL ? GEL : values()[ordinal() + 1];
        }
    }

    public static final EnumProperty<GelStage> STAGE = EnumProperty.create("stage", GelStage.class);
    /** How many water blocks each spreading gel block turns into gel. */
    private static final int SPREAD_COUNT = 3;

    public BlockWaterGel(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, GelStage.SPREAD_0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /** Turns the given water source block into gel, which starts spreading. */
    public void startGelling(Level level, BlockPos pos) {
        level.setBlock(pos, defaultBlockState(), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 200);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GelStage stage = state.getValue(STAGE);
        GelStage next = stage.next();
        BlockState nextState = state.setValue(STAGE, next);
        if (stage.spreading) {
            List<BlockPos> changeable = findWaterToSpreadTo(level, pos, random);
            int delay = next.spreading ? 200 : 400;
            if (changeable.size() == SPREAD_COUNT || random.nextDouble() < 0.5) {
                for (BlockPos p : changeable) {
                    level.setBlock(p, nextState, Block.UPDATE_ALL);
                    level.scheduleTick(p, this, random.nextInt(150) + delay);
                }
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                SoundType sound = next.soundType;
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2,
                    sound.getPitch() * 0.8f);
            }
            level.scheduleTick(pos, this, random.nextInt(150) + delay);
        } else if (stage != next) {
            // Gel only sets once there is no water around it anymore
            if (isTouchingWater(level, pos)) {
                level.scheduleTick(pos, this, random.nextInt(150) + 600);
            } else {
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                level.scheduleTick(pos, this, random.nextInt(150) + 400);
            }
        }
    }

    /** @return Up to 3 water source blocks near the given gel, found by searching through water and gel. */
    private List<BlockPos> findWaterToSpreadTo(Level level, BlockPos pos, RandomSource random) {
        Deque<BlockPos> openQueue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> changeable = new ArrayList<>();
        List<Direction> faces = new ArrayList<>(List.of(Direction.values()));
        seen.add(pos);
        shuffle(faces, random);
        for (Direction face : faces) {
            openQueue.add(pos.relative(face));
        }
        int tries = 0;
        while (!openQueue.isEmpty() && changeable.size() < SPREAD_COUNT && tries < 10_000) {
            BlockPos test = openQueue.removeFirst();
            BlockState testState = level.getBlockState(test);
            boolean water = testState.is(Blocks.WATER);
            if (water && testState.getFluidState().isSource()) {
                changeable.add(test);
            }
            if (water || testState.is(this)) {
                shuffle(faces, random);
                for (Direction face : faces) {
                    BlockPos n = test.relative(face);
                    if (seen.add(n)) {
                        openQueue.add(n);
                    }
                }
            }
            tries++;
        }
        return changeable;
    }

    private static void shuffle(List<Direction> faces, RandomSource random) {
        for (int i = faces.size() - 1; i > 0; i--) {
            Collections.swap(faces, i, random.nextInt(i + 1));
        }
    }

    private static boolean isTouchingWater(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (level.getBlockState(pos.relative(face)).is(Blocks.WATER)) {
                return true;
            }
        }
        return false;
    }

    // Each stage sounds and breaks a little differently

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return state.getValue(STAGE).soundType;
    }

    /** Same as vanilla, but with the hardness of the stage. */
    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = state.getValue(STAGE).hardness;
        int divider = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / divider;
    }
}
