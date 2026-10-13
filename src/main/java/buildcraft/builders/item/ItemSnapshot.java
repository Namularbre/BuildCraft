/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import buildcraft.builders.snapshot.Snapshot;

/** A blank template or blueprint, or one that holds a snapshot (made by an architect table). Only the key of the
 * snapshot is in the item: the snapshot itself is stored with the world. */
public class ItemSnapshot extends Item {
    public final Snapshot.Type type;

    public ItemSnapshot(Properties properties, Snapshot.Type type) {
        super(properties);
        this.type = type;
    }

    /** What is known about the snapshot of a used item. */
    public record Header(String key, String name, UUID owner, String ownerName, long created) {
        public CompoundTag write() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("key", key);
            nbt.putString("name", name);
            nbt.putUUID("owner", owner);
            nbt.putString("ownerName", ownerName);
            nbt.putLong("created", created);
            return nbt;
        }

        public static Header read(CompoundTag nbt) {
            UUID owner = nbt.hasUUID("owner") ? nbt.getUUID("owner") : new UUID(0, 0);
            return new Header(nbt.getString("key"), nbt.getString("name"), owner, nbt.getString("ownerName"),
                nbt.getLong("created"));
        }
    }

    @Nullable
    public static Header getHeader(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (stack.getItem() instanceof ItemSnapshot && tag != null && tag.contains("header", Tag.TAG_COMPOUND)) {
            return Header.read(tag.getCompound("header"));
        }
        return null;
    }

    public ItemStack createUsed(Header header) {
        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().put("header", header.write());
        return stack;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getHeader(stack) == null ? 16 : 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Header header = getHeader(stack);
        if (header == null) {
            tooltip.add(Component.translatable("buildcraft.snapshot.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!header.name().isEmpty()) {
            tooltip.add(Component.literal(header.name()));
        }
        if (!header.ownerName().isEmpty()) {
            tooltip.add(Component.translatable("buildcraft.snapshot.author", header.ownerName())
                .withStyle(ChatFormatting.GRAY));
        }
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("Key: " + header.key().substring(0, Math.min(16, header.key().length())))
                .withStyle(ChatFormatting.DARK_GRAY));
            String date = DateFormat.getDateTimeInstance().format(new Date(header.created()));
            tooltip.add(Component.literal("Date: " + date)
                .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
