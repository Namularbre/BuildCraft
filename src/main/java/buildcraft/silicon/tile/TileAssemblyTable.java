/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.recipes.AssemblyRecipe;

import buildcraft.lib.inventory.InventoryUtil;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.recipe.AssemblyRecipeRegistry;
import buildcraft.lib.tile.TileBC;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.container.ContainerAssemblyTable;

/** Makes the items of assembly recipes (like chipsets) from the items put in it, with the power of lasers. The player
 * chooses which of the possible recipes to make, and the table makes them in turn. */
public class TileAssemblyTable extends TileLaserTableBase {
    public static final IdAllocator IDS = TileBC.IDS.makeChild("assembly_table");
    public static final int NET_RECIPE_STATE = IDS.allocId("RECIPE_STATE");

    /** A recipe, and which of its outputs to make. */
    public record AssemblyInstruction(AssemblyRecipe recipe, ItemStack output) {
        private static final Comparator<AssemblyInstruction> ORDER = Comparator
            .comparing((AssemblyInstruction i) -> i.recipe)
            .thenComparing(i -> String.valueOf(ForgeRegistries.ITEMS.getKey(i.output.getItem())))
            .thenComparing(i -> String.valueOf(i.output.getTag()));

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AssemblyInstruction other && recipe.equals(other.recipe)
                && ItemStack.matches(output, other.output);
        }

        @Override
        public int hashCode() {
            return recipe.hashCode() * 31 + output.getItem().hashCode();
        }
    }

    public final ItemStackHandler inv = new ItemStackHandler(3 * 4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** The recipes that can be made (or that the player saved), in the order shown in the GUI. */
    public final SortedMap<AssemblyInstruction, EnumAssemblyRecipeState> recipesStates =
        new TreeMap<>(AssemblyInstruction.ORDER);

    public TileAssemblyTable(BlockPos pos, BlockState state) {
        super(BCSiliconBlocks.ASSEMBLY_TABLE_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.ITEM_HANDLER, inv, EnumPipePart.VALUES);
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    private List<ItemStack> getStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) {
            stacks.add(inv.getStackInSlot(i));
        }
        return stacks;
    }

    /** Adds the recipes that the items allow, forgets the unsaved ones that they don't allow anymore, and makes sure
     * that one saved recipe (with enough items) is active. */
    private void updateRecipes() {
        int count = recipesStates.size();
        List<ItemStack> stacks = getStacks();
        for (AssemblyRecipe recipe : AssemblyRecipeRegistry.REGISTRY.values()) {
            for (ItemStack out : recipe.getOutputs(stacks)) {
                recipesStates.putIfAbsent(new AssemblyInstruction(recipe, out), EnumAssemblyRecipeState.POSSIBLE);
            }
        }

        boolean foundActive = false;
        var iterator = recipesStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry = iterator.next();
            AssemblyInstruction instruction = entry.getKey();
            EnumAssemblyRecipeState state = entry.getValue();
            boolean enough = extract(inv, instruction.recipe.getInputsFor(instruction.output), true);
            if (state == EnumAssemblyRecipeState.POSSIBLE) {
                if (!enough) {
                    iterator.remove();
                }
                continue;
            }
            if (!enough) {
                state = EnumAssemblyRecipeState.SAVED;
            } else if (state == EnumAssemblyRecipeState.SAVED) {
                state = EnumAssemblyRecipeState.SAVED_ENOUGH;
            }
            foundActive |= state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE;
            entry.setValue(state);
        }
        if (!foundActive) {
            for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
                if (entry.getValue() == EnumAssemblyRecipeState.SAVED_ENOUGH) {
                    entry.setValue(EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE);
                    break;
                }
            }
        }
        if (count != recipesStates.size()) {
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    @Nullable
    private AssemblyInstruction getActiveRecipe() {
        for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
            if (entry.getValue() == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Makes the next saved recipe with enough items (after the active one, going round) the active one. */
    private void activateNextRecipe() {
        List<Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState>> entries =
            new ArrayList<>(recipesStates.entrySet());
        int active = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getValue() == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                active = i;
            }
        }
        if (active < 0) {
            return;
        }
        for (int offset = 1; offset < entries.size(); offset++) {
            Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> next =
                entries.get((active + offset) % entries.size());
            if (next.getValue() == EnumAssemblyRecipeState.SAVED_ENOUGH) {
                entries.get(active).setValue(EnumAssemblyRecipeState.SAVED_ENOUGH);
                next.setValue(EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE);
                return;
            }
        }
    }

    @Override
    public long getTarget() {
        AssemblyInstruction active = getActiveRecipe();
        return active == null ? 0 : active.recipe.getRequiredMicroJoulesFor(active.output);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            return;
        }
        updateRecipes();
        long target = getTarget();
        if (target > 0) {
            if (power >= target) {
                AssemblyInstruction instruction = getActiveRecipe();
                extract(inv, instruction.recipe.getInputsFor(instruction.output), false);
                InventoryUtil.addToBestAcceptor(level, worldPosition, instruction.output.copy());
                power -= target;
                activateNextRecipe();
            }
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    /** Saves the given recipe (so that the table makes it), or forgets it. Called from the GUI. */
    public void setRecipeState(AssemblyInstruction instruction, EnumAssemblyRecipeState state) {
        if (level.isClientSide) {
            MessageManager.sendToServer(createMessage(NET_RECIPE_STATE, buffer -> {
                writeInstruction(buffer, instruction);
                buffer.writeEnum(state);
            }));
        } else if (recipesStates.containsKey(instruction)) {
            recipesStates.put(instruction, state);
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    @Override
    protected Iterable<ItemStack> collectDrops() {
        return getStacks();
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (isHoldingTool(player, hand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, Component.translatable(getBlockState().getBlock().getDescriptionId()),
                (id, inventory, p) -> new ContainerAssemblyTable(id, p, this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Saving

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("items", inv.serializeNBT());
        ListTag list = new ListTag();
        recipesStates.forEach((instruction, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("recipe", instruction.recipe.getRegistryName().toString());
            entry.put("output", instruction.output.save(new CompoundTag()));
            entry.putInt("state", state.ordinal());
            list.add(entry);
        });
        nbt.put("recipes_states", list);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        inv.deserializeNBT(nbt.getCompound("items"));
        recipesStates.clear();
        ListTag list = nbt.getList("recipes_states", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            AssemblyInstruction instruction =
                lookupRecipe(entry.getString("recipe"), ItemStack.of(entry.getCompound("output")));
            int state = entry.getInt("state");
            if (instruction != null && state >= 0 && state < EnumAssemblyRecipeState.values().length) {
                recipesStates.put(instruction, EnumAssemblyRecipeState.values()[state]);
            }
        }
    }

    // Networking

    private static void writeInstruction(FriendlyByteBuf buffer, AssemblyInstruction instruction) {
        buffer.writeResourceLocation(instruction.recipe.getRegistryName());
        buffer.writeItem(instruction.output);
    }

    @Nullable
    private static AssemblyInstruction readInstruction(FriendlyByteBuf buffer) {
        return lookupRecipe(buffer.readResourceLocation().toString(), buffer.readItem());
    }

    @Nullable
    private static AssemblyInstruction lookupRecipe(String name, ItemStack output) {
        AssemblyRecipe recipe = AssemblyRecipeRegistry.REGISTRY.get(ResourceLocation.tryParse(name));
        return recipe == null ? null : new AssemblyInstruction(recipe, output);
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && id == NET_GUI_DATA) {
            buffer.writeVarInt(recipesStates.size());
            recipesStates.forEach((instruction, state) -> {
                writeInstruction(buffer, instruction);
                buffer.writeEnum(state);
            });
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readPayload(id, buffer, side, sender);
        if (side == LogicalSide.CLIENT && id == NET_GUI_DATA) {
            recipesStates.clear();
            int count = buffer.readVarInt();
            for (int i = 0; i < count; i++) {
                AssemblyInstruction instruction = readInstruction(buffer);
                EnumAssemblyRecipeState state = buffer.readEnum(EnumAssemblyRecipeState.class);
                if (instruction != null) {
                    recipesStates.put(instruction, state);
                }
            }
        } else if (side == LogicalSide.SERVER && id == NET_RECIPE_STATE) {
            AssemblyInstruction instruction = readInstruction(buffer);
            EnumAssemblyRecipeState state = buffer.readEnum(EnumAssemblyRecipeState.class);
            if (instruction != null && sender != null && canInteractWith(sender)) {
                setRecipeState(instruction, state);
            }
        }
    }
}
