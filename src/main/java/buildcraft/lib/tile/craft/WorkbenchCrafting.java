/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.tile.craft;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import buildcraft.lib.inventory.InventoryUtil;

/** Crafts the recipe shown in a blueprint inventory, taking the exact items shown from a materials inventory, like a
 * player would at a crafting table. */
public class WorkbenchCrafting {
    /** Crafting grids need a menu to tell about changes: nothing listens here. */
    private static final AbstractContainerMenu NULL_MENU = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void slotsChanged(Container container) {}
    };

    private final BlockEntity tile;
    private final IItemHandlerModifiable invBlueprint, invMaterials, invResult;
    private final TransientCraftingContainer grid;

    private boolean isBlueprintDirty = true;
    private boolean areMaterialsDirty = true;
    private boolean cachedHasRequirements = false;
    @Nullable
    private CraftingRecipe currentRecipe;
    private ItemStack assumedResult = ItemStack.EMPTY;

    public WorkbenchCrafting(int width, int height, BlockEntity tile, IItemHandlerModifiable invBlueprint,
        IItemHandlerModifiable invMaterials, IItemHandlerModifiable invResult) {
        if (invBlueprint.getSlots() < width * height) {
            throw new IllegalArgumentException("The blueprint is smaller than width * height! (expected "
                + width * height + ", got " + invBlueprint.getSlots() + ")");
        }
        this.tile = tile;
        this.invBlueprint = invBlueprint;
        this.invMaterials = invMaterials;
        this.invResult = invResult;
        this.grid = new TransientCraftingContainer(NULL_MENU, width, height);
    }

    /** @return What the blueprint makes, or nothing if it isn't a recipe. */
    public ItemStack getAssumedResult() {
        return assumedResult;
    }

    public void onInventoryChange(IItemHandler inv) {
        if (inv == invBlueprint) {
            isBlueprintDirty = true;
        } else if (inv == invMaterials) {
            areMaterialsDirty = true;
        }
    }

    /** Looks for the recipe again if the blueprint changed. Server side only.
     *
     * @return True if anything changed. */
    public boolean tick() {
        if (!isBlueprintDirty) {
            return false;
        }
        for (int s = 0; s < grid.getContainerSize(); s++) {
            grid.setItem(s, invBlueprint.getStackInSlot(s).copy());
        }
        currentRecipe = tile.getLevel().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, grid, tile.getLevel()).orElse(null);
        assumedResult = currentRecipe == null ? ItemStack.EMPTY
            : currentRecipe.assemble(grid, tile.getLevel().registryAccess());
        grid.clearContent();
        isBlueprintDirty = false;
        areMaterialsDirty = true;
        return true;
    }

    /** @return True if {@link #craft()} might succeed, or false if it definitely won't. */
    public boolean canCraft() {
        if (currentRecipe == null || isBlueprintDirty) {
            return false;
        }
        if (!ItemHandlerHelper.insertItemStacked(invResult, assumedResult, true).isEmpty()) {
            return false;
        }
        if (areMaterialsDirty) {
            areMaterialsDirty = false;
            cachedHasRequirements = hasExactStacks();
        }
        return cachedHasRequirements;
    }

    /** @return True if the materials hold every item of the blueprint. */
    private boolean hasExactStacks() {
        List<ItemStack> kinds = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int s = 0; s < grid.getContainerSize(); s++) {
            ItemStack required = invBlueprint.getStackInSlot(s);
            if (required.isEmpty()) {
                continue;
            }
            int index = indexOf(kinds, required);
            if (index < 0) {
                kinds.add(required);
                counts.add(1);
            } else {
                counts.set(index, counts.get(index) + 1);
            }
        }
        for (int i = 0; i < kinds.size(); i++) {
            int available = 0;
            for (int s = 0; s < invMaterials.getSlots(); s++) {
                ItemStack inSlot = invMaterials.getStackInSlot(s);
                if (ItemStack.isSameItemSameTags(inSlot, kinds.get(i))) {
                    available += inSlot.getCount();
                }
            }
            if (available < counts.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(List<ItemStack> kinds, ItemStack stack) {
        for (int i = 0; i < kinds.size(); i++) {
            if (ItemStack.isSameItemSameTags(kinds.get(i), stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Crafts the recipe once: the result goes in the result inventory, and what is left in the grid (like empty
     * buckets) goes back to the materials, or out of the machine.
     *
     * @return True if something was crafted. */
    public boolean craft() {
        if (isBlueprintDirty || currentRecipe == null) {
            return false;
        }
        for (int s = 0; s < grid.getContainerSize(); s++) {
            ItemStack required = invBlueprint.getStackInSlot(s);
            if (!required.isEmpty()) {
                ItemStack stack = extractOne(required);
                if (stack.isEmpty()) {
                    emptyGrid();
                    return false;
                }
                grid.setItem(s, stack);
            }
        }
        // Some recipes (like fireworks) store what matches finds for assemble
        if (!currentRecipe.matches(grid, tile.getLevel())) {
            emptyGrid();
            return false;
        }
        ItemStack result = currentRecipe.assemble(grid, tile.getLevel().registryAccess());
        if (result.isEmpty()) {
            emptyGrid();
            return false;
        }
        expel(ItemHandlerHelper.insertItemStacked(invResult, result, false));

        NonNullList<ItemStack> remaining = currentRecipe.getRemainingItems(grid);
        for (int s = 0; s < grid.getContainerSize(); s++) {
            grid.removeItem(s, 1);
            ItemStack left = remaining.get(s);
            if (!left.isEmpty()) {
                expel(ItemHandlerHelper.insertItemStacked(invMaterials, left, false));
            }
        }
        emptyGrid();
        return true;
    }

    private ItemStack extractOne(ItemStack like) {
        for (int s = 0; s < invMaterials.getSlots(); s++) {
            if (ItemStack.isSameItemSameTags(invMaterials.getStackInSlot(s), like)) {
                return invMaterials.extractItem(s, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Moves everything still in the grid back to the materials (or out of the machine). */
    private void emptyGrid() {
        for (int s = 0; s < grid.getContainerSize(); s++) {
            ItemStack inSlot = grid.removeItemNoUpdate(s);
            if (!inSlot.isEmpty()) {
                expel(ItemHandlerHelper.insertItemStacked(invMaterials, inSlot, false));
            }
        }
    }

    /** Gives items that don't fit anywhere in the machine to its neighbours, or drops them. */
    private void expel(ItemStack stack) {
        if (!stack.isEmpty()) {
            InventoryUtil.addToBestAcceptor(tile.getLevel(), tile.getBlockPos(), stack);
        }
    }
}
