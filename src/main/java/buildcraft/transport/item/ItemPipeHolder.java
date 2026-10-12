/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.item;

import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.PipeDefinition;

/** Places a pipe holder containing a pipe of the given definition. */
public class ItemPipeHolder extends BlockItem implements IItemPipe {
    public final PipeDefinition definition;

    public ItemPipeHolder(Block pipeHolder, PipeDefinition definition, Properties properties) {
        super(pipeHolder, properties);
        this.definition = definition;
    }

    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getDescriptionId() {
        // BlockItem uses the block's name, but every pipe has its own
        return getOrCreateDescriptionId();
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        // Every pipe item places the same block, so none of them is "the" item of that block
    }

    @Override
    public void removeFromBlockToItemMap(Map<Block, Item> blockToItemMap, Item item) {}
}
