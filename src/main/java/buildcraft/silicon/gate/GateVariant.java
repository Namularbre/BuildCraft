/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** A kind of gate: its logic, material and modifier. Every variant is a separate item. */
public record GateVariant(EnumGateLogic logic, EnumGateMaterial material, EnumGateModifier modifier) {
    /** Every variant that exists: the basic (clay brick) gate, then every combination of the other materials. */
    public static final List<GateVariant> VARIANTS;

    static {
        List<GateVariant> variants = new ArrayList<>();
        variants.add(new GateVariant(EnumGateLogic.AND, EnumGateMaterial.CLAY_BRICK, EnumGateModifier.NO_MODIFIER));
        for (EnumGateMaterial material : EnumGateMaterial.VALUES) {
            if (!material.canBeModified) {
                continue;
            }
            for (EnumGateLogic logic : EnumGateLogic.VALUES) {
                for (EnumGateModifier modifier : EnumGateModifier.VALUES) {
                    variants.add(new GateVariant(logic, material, modifier));
                }
            }
        }
        VARIANTS = Collections.unmodifiableList(variants);
    }

    public GateVariant {
        if (!material.canBeModified) {
            // The basic gate only exists in one variant
            logic = EnumGateLogic.AND;
            modifier = EnumGateModifier.NO_MODIFIER;
        }
    }

    public static GateVariant readFromNbt(CompoundTag nbt) {
        return new GateVariant(EnumGateLogic.getByOrdinal(nbt.getByte("logic")),
            EnumGateMaterial.getByOrdinal(nbt.getByte("material")),
            EnumGateModifier.getByOrdinal(nbt.getByte("modifier")));
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("logic", (byte) logic.ordinal());
        nbt.putByte("material", (byte) material.ordinal());
        nbt.putByte("modifier", (byte) modifier.ordinal());
        return nbt;
    }

    public static GateVariant readFromBuffer(FriendlyByteBuf buffer) {
        return new GateVariant(EnumGateLogic.getByOrdinal(buffer.readUnsignedByte()),
            EnumGateMaterial.getByOrdinal(buffer.readUnsignedByte()),
            EnumGateModifier.getByOrdinal(buffer.readUnsignedByte()));
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
        buffer.writeByte(logic.ordinal());
        buffer.writeByte(material.ordinal());
        buffer.writeByte(modifier.ordinal());
    }

    /** @return The number of trigger/action pairs. */
    public int numSlots() {
        return material.numSlots / modifier.slotDivisor;
    }

    public int numTriggerArgs() {
        return modifier.triggerParams;
    }

    public int numActionArgs() {
        return modifier.actionParams;
    }

    /** @return The name of the item of this variant (like "gate_iron_and_lapis"). */
    public String getItemName() {
        if (!material.canBeModified) {
            return "gate_" + material.tag;
        }
        String name = "gate_" + material.tag + "_" + logic.tag;
        return modifier == EnumGateModifier.NO_MODIFIER ? name : name + "_" + modifier.tag;
    }

    public Component getDisplayName() {
        if (!material.canBeModified) {
            return Component.translatable("buildcraft.gate.name.basic");
        }
        Component name = Component.translatable("buildcraft.gate.name",
            Component.translatable("buildcraft.gate.material." + material.tag),
            Component.translatable("buildcraft.gate.logic." + logic.tag));
        if (modifier == EnumGateModifier.NO_MODIFIER) {
            return name;
        }
        return Component.translatable("buildcraft.gate.name.modified", name,
            Component.translatable("buildcraft.gate.modifier." + modifier.tag));
    }
}
