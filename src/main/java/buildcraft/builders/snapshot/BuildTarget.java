/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import net.minecraftforge.items.IItemHandler;

import buildcraft.lib.misc.data.Box;

/** What a {@link SnapshotBuilder} builds: a box of blocks (in the snapshot's coordinates), placed somewhere in the
 * world, turned by some rotation. */
public abstract class BuildTarget {
    public enum Result {
        CORRECT,
        TO_BREAK,
        TO_PLACE
    }

    /** The size of the snapshot (before being turned). */
    public final BlockPos size;
    /** Where the snapshot's 0, 0, 0 goes in the world. */
    public final BlockPos origin;
    public final Rotation rotation;
    /** The box (in the world) that is built. */
    public final Box box;

    protected BuildTarget(BlockPos size, BlockPos origin, Rotation rotation) {
        this.size = size;
        this.origin = origin;
        this.rotation = rotation;
        BlockPos a = toWorld(BlockPos.ZERO);
        BlockPos b = toWorld(size.offset(-1, -1, -1));
        this.box = new Box(new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())), new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())));
    }

    public int count() {
        return size.getX() * size.getY() * size.getZ();
    }

    public BlockPos toWorld(BlockPos local) {
        return local.rotate(rotation).offset(origin);
    }

    /** @return The index of the given world position, or -1 if it isn't in the box. */
    public int indexOf(BlockPos world) {
        BlockPos local = world.subtract(origin).rotate(invert(rotation));
        if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 || local.getX() >= size.getX()
            || local.getY() >= size.getY() || local.getZ() >= size.getZ()) {
            return -1;
        }
        return local.getX() + size.getX() * (local.getY() + size.getY() * local.getZ());
    }

    public BlockPos worldPos(int index) {
        int x = index % size.getX();
        int rest = index / size.getX();
        return toWorld(new BlockPos(x, rest % size.getY(), rest / size.getY()));
    }

    private static Rotation invert(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    /** @return What has to be done at the given position. */
    public abstract Result check(Level level, int index, BlockPos pos);

    /** Takes the items needed to place the block at the given index out of the inventory.
     *
     * @return The items (an empty stack if none are needed), or null if they aren't all there. */
    @Nullable
    public abstract ItemStack takeItems(IItemHandler inventory, int index);

    /** @return The items needed to place the block at the given index (for showing them in GUIs). */
    public abstract List<ItemStack> getRequiredItems(int index);

    /** @return True if the block was placed. */
    public abstract boolean place(ServerLevel level, int index, BlockPos pos, ItemStack items, GameProfile owner);
}
