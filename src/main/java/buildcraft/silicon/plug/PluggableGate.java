/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkHooks;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.transport.tile.TilePipeHolder;

/** A gate on the side of a pipe. */
public class PluggableGate extends PipePluggable {
    private static final AABB[] BOXES = new AABB[6];

    static {
        double ll = 2 / 16.0;
        double lu = 4 / 16.0;
        double ul = 12 / 16.0;
        double uu = 14 / 16.0;
        double min = 5 / 16.0;
        double max = 11 / 16.0;
        BOXES[Direction.DOWN.ordinal()] = new AABB(min, ll, min, max, lu, max);
        BOXES[Direction.UP.ordinal()] = new AABB(min, ul, min, max, uu, max);
        BOXES[Direction.NORTH.ordinal()] = new AABB(min, min, ll, max, max, lu);
        BOXES[Direction.SOUTH.ordinal()] = new AABB(min, min, ul, max, max, uu);
        BOXES[Direction.WEST.ordinal()] = new AABB(ll, min, min, lu, max, max);
        BOXES[Direction.EAST.ordinal()] = new AABB(ul, min, min, uu, max, max);
    }

    public final GateLogic logic;

    // Manual constructor (called by the gate item)
    public PluggableGate(PluggableDefinition def, IPipeHolder holder, Direction side, GateVariant variant) {
        super(def, holder, side);
        logic = new GateLogic(this, variant);
    }

    // Saving + Loading
    public PluggableGate(PluggableDefinition def, IPipeHolder holder, Direction side, CompoundTag nbt) {
        super(def, holder, side);
        logic = new GateLogic(this, nbt.getCompound("data"));
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.put("data", logic.writeToNbt());
        return nbt;
    }

    // Networking
    public PluggableGate(PluggableDefinition def, IPipeHolder holder, Direction side, FriendlyByteBuf buffer) {
        super(def, holder, side);
        logic = new GateLogic(this, buffer);
    }

    @Override
    public void writeCreationPayload(FriendlyByteBuf buffer) {
        logic.writeCreationToBuf(buffer);
    }

    @Override
    public void readPayload(FriendlyByteBuf buffer, LogicalSide side) {
        logic.readPayload(buffer, side);
    }

    // PipePluggable

    @Override
    public AABB getBoundingBox() {
        return BOXES[side.ordinal()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCSiliconItems.gate(logic.variant));
    }

    @Override
    public PluggableModelKey getModelRenderKey() {
        return new KeyPlugGate(side, logic.variant, logic.isOn);
    }

    @Override
    public boolean onPluggableActivate(Player player, HitResult trace, float hitX, float hitY, float hitZ) {
        if (player instanceof ServerPlayer serverPlayer && holder.getPipeTile() instanceof TilePipeHolder tile) {
            Component title = logic.variant.getDisplayName();
            NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((id, inventory, p) -> new ContainerGate(id, p, tile, side), title), buffer -> {
                    buffer.writeBlockPos(tile.getBlockPos());
                    buffer.writeEnum(side);
                });
        }
        return true;
    }

    @Override
    public void onTick() {
        logic.onTick();
    }

    @Override
    public boolean canConnectToRedstone() {
        return true;
    }

    /** The model of a gate: it depends on the variant, and on whether the gate is on. */
    public static final class KeyPlugGate extends PluggableModelKey {
        public final GateVariant variant;
        public final boolean on;

        public KeyPlugGate(Direction side, GateVariant variant, boolean on) {
            super(side);
            this.variant = variant;
            this.on = on;
        }

        @Override
        public boolean equals(Object obj) {
            return super.equals(obj) && ((KeyPlugGate) obj).variant.equals(variant) && ((KeyPlugGate) obj).on == on;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), variant, on);
        }
    }
}
