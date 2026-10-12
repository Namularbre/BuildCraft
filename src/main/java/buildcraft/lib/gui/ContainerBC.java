/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fml.LogicalSide;

import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.IPayloadWriter;
import buildcraft.lib.net.MessageContainer;
import buildcraft.lib.net.MessageManager;

/** Base class for every BuildCraft menu. Adds the player inventory helpers, a generic shift-click implementation, and
 * id-tagged messages between both sides of the menu. */
public abstract class ContainerBC extends AbstractContainerMenu {
    protected static final IdAllocator IDS = new IdAllocator("container");
    /** Generic "data" id. Used by all containers which only have 1 id to write out (no point in making EVERY container
     * have an {@link IdAllocator} if they only allocate one. */
    public static final int NET_DATA = IDS.allocId("DATA");

    private static final int PLAYER_INVENTORY_SIZE = 36;

    public final Player player;

    protected ContainerBC(@Nullable MenuType<?> type, int containerId, Player player) {
        super(type, containerId);
        this.player = player;
    }

    /** @return The {@link IdAllocator} that allocates all ID's for this class, and its parent classes. All subclasses
     *         should override this if they allocate their own ids after calling
     *         {@link IdAllocator#makeChild(String)} */
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    protected void addFullPlayerInventory(int startX, int startY) {
        Inventory inventory = player.getInventory();
        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlot(new Slot(inventory, sx + sy * 9 + 9, startX + sx * 18, startY + sy * 18));
            }
        }

        for (int sx = 0; sx < 9; sx++) {
            addSlot(new Slot(inventory, sx, startX + sx * 18, startY + 58));
        }
    }

    protected void addFullPlayerInventory(int startY) {
        addFullPlayerInventory(8, startY);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof SlotPhantom phantom) {
            if (clickType == ClickType.PICKUP) {
                phantom.setPhantom(getCarried());
            } else if (clickType == ClickType.QUICK_MOVE) {
                phantom.setPhantom(ItemStack.EMPTY);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slots.size() == PLAYER_INVENTORY_SIZE) return ItemStack.EMPTY;
        boolean playerInventoryFirst = slots.get(0).container instanceof Inventory;

        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (playerInventoryFirst) {
                if (index < PLAYER_INVENTORY_SIZE) {
                    if (!this.moveItemStackTo(itemstack1, PLAYER_INVENTORY_SIZE, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, PLAYER_INVENTORY_SIZE, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                int firstPlayerSlot = this.slots.size() - PLAYER_INVENTORY_SIZE;
                if (index < firstPlayerSlot) {
                    if (!this.moveItemStackTo(itemstack1, firstPlayerSlot, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, firstPlayerSlot, true)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public final void sendMessage(int id) {
        LogicalSide side = player.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
        sendMessage(id, (buffer) -> writeMessage(id, buffer, side));
    }

    public final void sendMessage(int id, IPayloadWriter writer) {
        FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.buffer());
        writer.write(payload);
        MessageContainer message = new MessageContainer(containerId, id, payload);
        if (player.level().isClientSide) {
            MessageManager.sendToServer(message);
        } else if (player instanceof ServerPlayer serverPlayer) {
            MessageManager.sendTo(message, serverPlayer);
        }
    }

    public void writeMessage(int id, FriendlyByteBuf buffer, LogicalSide side) {}

    /** @param sender The player that sent the message, or null if it was sent by the server. */
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {}
}
