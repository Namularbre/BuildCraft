/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags used by BuildCraft. Shared materials use Forge's "forge" namespace conventions so that other mods' gears and
 * wrenches work too. */
public final class BCTags {
    private BCTags() {}

    public static final class Items {
        public static final TagKey<Item> GEARS = forge("gears");
        public static final TagKey<Item> GEARS_WOOD = forge("gears/wood");
        public static final TagKey<Item> GEARS_STONE = forge("gears/stone");
        public static final TagKey<Item> GEARS_IRON = forge("gears/iron");
        public static final TagKey<Item> GEARS_GOLD = forge("gears/gold");
        public static final TagKey<Item> GEARS_DIAMOND = forge("gears/diamond");
        public static final TagKey<Item> TOOLS = forge("tools");
        public static final TagKey<Item> WRENCHES = forge("tools/wrench");

        private Items() {}

        private static TagKey<Item> forge(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path));
        }
    }
}
