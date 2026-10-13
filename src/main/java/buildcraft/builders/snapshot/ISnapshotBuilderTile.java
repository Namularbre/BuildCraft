/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.items.IItemHandler;

import buildcraft.api.mj.MjBattery;

/** A machine that builds with a {@link SnapshotBuilder} (like the filler and the builder). */
public interface ISnapshotBuilderTile {
    Level getLevel();

    BlockPos getBlockPos();

    MjBattery getBattery();

    boolean canExcavate();

    GameProfile getOwner();

    /** @return The items used to build. */
    IItemHandler getResources();

    /** Puts an item back into the resources (or drops it if it doesn't fit). */
    void returnItem(ItemStack stack);
}
