/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.gui;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.core.statements.BCStatement;
import buildcraft.transport.container.ContainerEmzuliPipe;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;

/** The 4 filters of an emzuli pipe, each with a button that chooses the colour that its items are painted with: left
 * click for the next colour, right click for the previous one, and middle click for no colour. The filters that a gate
 * activates have a lock next to them (and the one used next a tick). */
public class GuiEmzuliPipe extends GuiBC<ContainerEmzuliPipe> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/pipe_emzuli.png");
    private static final ResourceLocation LOCK =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/icons/lock.png");
    /** Where the paint button of each {@link SlotIndex} is. */
    private static final int[][] BUTTONS = { { 49, 19 }, { 49, 47 }, { 106, 19 }, { 106, 47 } };
    private static final int BUTTON_SIZE = 20;

    public GuiEmzuliPipe(ContainerEmzuliPipe container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    private PipeBehaviourEmzuli behaviour() {
        return menu.behaviour;
    }

    @Nullable
    private SlotIndex getButtonAt(double mouseX, double mouseY) {
        for (SlotIndex index : SlotIndex.VALUES) {
            double x = mouseX - leftPos - BUTTONS[index.ordinal()][0];
            double y = mouseY - topPos - BUTTONS[index.ordinal()][1];
            if (x >= 0 && y >= 0 && x < BUTTON_SIZE && y < BUTTON_SIZE) {
                return index;
            }
        }
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        SlotIndex hovered = getButtonAt(mouseX, mouseY);
        for (SlotIndex index : SlotIndex.VALUES) {
            int x = leftPos + BUTTONS[index.ordinal()][0];
            int y = topPos + BUTTONS[index.ordinal()][1];
            graphics.blit(TEXTURE, x, y, 176, index == hovered ? 20 : 0, BUTTON_SIZE, BUTTON_SIZE);
            DyeColor colour = behaviour().slotColours.get(index);
            if (colour == null) {
                graphics.blit(TEXTURE, x + 2, y + 2, 176, 40, 16, 16);
            } else {
                graphics.blit(BCStatement.sprite("pipe_colour_" + colour.getName()), x + 2, y + 2, 0, 0, 16, 16,
                    16, 16);
            }
        }
        SlotIndex current = behaviour().getCurrentSlot();
        for (SlotIndex index : behaviour().getActiveSlots()) {
            int x = leftPos + (index.ordinal() < 2 ? 4 : 155);
            int y = topPos + (index.ordinal() % 2 == 0 ? 21 : 49);
            ResourceLocation icon = index == current ? BCStatement.sprite("trigger_true") : LOCK;
            graphics.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        SlotIndex hovered = getButtonAt(mouseX, mouseY);
        if (hovered != null) {
            DyeColor colour = behaviour().slotColours.get(hovered);
            Component tip = colour == null ? Component.translatable("buildcraft.gui.emzuli_pipe.no_paint")
                : Component.translatable("buildcraft.gui.emzuli_pipe.paint",
                    Component.translatable("color.minecraft." + colour.getName()));
            graphics.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SlotIndex index = getButtonAt(mouseX, mouseY);
        if (index != null) {
            DyeColor old = behaviour().slotColours.get(index);
            DyeColor colour = switch (button) {
                case 0 -> next(old);
                case 1 -> previous(old);
                default -> null;
            };
            menu.setPaint(index, colour);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** @return The colour after the given one, going through no colour (null) after the last one. */
    @Nullable
    private static DyeColor next(@Nullable DyeColor colour) {
        if (colour == null) {
            return DyeColor.byId(0);
        }
        return colour.getId() == 15 ? null : DyeColor.byId(colour.getId() + 1);
    }

    @Nullable
    private static DyeColor previous(@Nullable DyeColor colour) {
        if (colour == null) {
            return DyeColor.byId(15);
        }
        return colour.getId() == 0 ? null : DyeColor.byId(colour.getId() - 1);
    }
}
