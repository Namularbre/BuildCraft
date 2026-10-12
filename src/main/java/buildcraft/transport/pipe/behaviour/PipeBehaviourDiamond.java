/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.items.ItemStackHandler;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;

import buildcraft.lib.tile.TileBC;

import buildcraft.transport.container.ContainerDiamondPipe;

/** A pipe with 9 filters per side (set in its GUI): what matches the filters of a side goes that way. */
public abstract class PipeBehaviourDiamond extends PipeBehaviour {
    public static final int FILTERS_PER_SIDE = 9;

    public final ItemStackHandler filters = new ItemStackHandler(FILTERS_PER_SIDE * 6) {
        @Override
        protected void onContentsChanged(int slot) {
            pipe.getHolder().getPipeTile().setChanged();
        }
    };

    public PipeBehaviourDiamond(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviourDiamond(IPipe pipe, CompoundTag nbt) {
        super(pipe, nbt);
        filters.deserializeNBT(nbt.getCompound("filters"));
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("filters", filters.serializeNBT());
        return nbt;
    }

    /** Each side has its own texture (of the colour of its filter row in the GUI). */
    @Override
    public int getTextureIndex(@Nullable Direction face) {
        return face == null ? 0 : face.ordinal() + 1;
    }

    @Override
    public boolean onPipeActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ,
        EnumPipePart part) {
        if (player instanceof ServerPlayer serverPlayer && pipe.getHolder().getPipeTile() instanceof TileBC tile) {
            tile.openMenu(serverPlayer, Component.translatable("buildcraft.gui.diamond_pipe"),
                (id, inventory, p) -> new ContainerDiamondPipe(id, p, tile));
        }
        return true;
    }
}
