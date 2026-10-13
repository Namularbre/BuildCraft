/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.GuiBC;

import buildcraft.BuildCraft;
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.tile.TileBuilder;

/** The GUI of the builder: the snapshot slot, the blocks to build with, and (on the right) the items that are still
 * needed for blueprints. */
public class GuiBuilder extends GuiBC<ContainerBuilder> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/builder.png");
    private static final ResourceLocation TEXTURE_BLUEPRINT =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/builder_blueprint.png");
    /** The buttons are the same as the filler's. */
    private static final ResourceLocation TEXTURE_BUTTONS =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/filler.png");
    private static final int PANEL_X = 169, PANEL_WIDTH = 87, PANEL_HEIGHT = 136;
    private static final int EXCAVATE_X = 152, EXCAVATE_Y = 27;

    public GuiBuilder(ContainerBuilder container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 256, 222);
        inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (176 - font.width(title)) / 2;
    }

    private TileBuilder tile() {
        return menu.tile;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 176, 222);
        // The list of required items (without the tanks: fluids aren't built)
        graphics.blit(TEXTURE_BLUEPRINT, leftPos + PANEL_X, topPos, PANEL_X, 0, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.blit(TEXTURE_BLUEPRINT, leftPos + PANEL_X, topPos + PANEL_HEIGHT, PANEL_X, 215, PANEL_WIDTH, 7);
        boolean hovered = isIn(mouseX, mouseY, EXCAVATE_X, EXCAVATE_Y);
        graphics.blit(TEXTURE_BUTTONS, leftPos + EXCAVATE_X, topPos + EXCAVATE_Y, tile().canExcavate() ? 208 : 192,
            hovered ? 16 : 0, 16, 16);
    }

    private boolean isIn(double mouseX, double mouseY, int x, int y) {
        return mouseX >= leftPos + x && mouseY >= topPos + y && mouseX < leftPos + x + 16 && mouseY < topPos + y + 16;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("buildcraft.gui.builder.resources"), 8, 61, 0x40_40_40, false);
        Component status;
        if (tile().clientType == null) {
            status = Component.translatable("buildcraft.gui.builder.empty");
        } else if (tile().clientFinished) {
            status = Component.translatable("buildcraft.gui.filler.finished");
        } else {
            status = Component.translatable("buildcraft.gui.filler.progress", tile().getBuilder().leftToBreak,
                tile().getBuilder().leftToPlace);
        }
        graphics.drawString(font, status, 8, 48, 0x40_40_40, false);
        List<ItemStack> required = tile().clientRequired;
        for (int i = 0; i < required.size() && i < TileBuilder.DISPLAYED_ITEMS; i++) {
            int x = 179 + (i % 4) * 18;
            int y = 18 + (i / 4) * 18;
            ItemStack stack = required.get(i);
            graphics.renderItem(stack, x, y);
            String count = stack.getCount() > 999 ? "999+" : Integer.toString(stack.getCount());
            graphics.renderItemDecorations(font, stack.copyWithCount(1), x, y, count);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isIn(mouseX, mouseY, EXCAVATE_X, EXCAVATE_Y)) {
            graphics.renderTooltip(font, Component.translatable("buildcraft.gui.filler.excavate."
                + tile().canExcavate()), mouseX, mouseY);
        }
        List<ItemStack> required = tile().clientRequired;
        for (int i = 0; i < required.size() && i < TileBuilder.DISPLAYED_ITEMS; i++) {
            int x = leftPos + 179 + (i % 4) * 18;
            int y = topPos + 18 + (i / 4) * 18;
            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16) {
                graphics.renderTooltip(font, required.get(i), mouseX, mouseY);
            }
        }
        if (tile().clientType == Snapshot.Type.TEMPLATE && isIn(mouseX, mouseY, 80, 27)) {
            graphics.renderTooltip(font, Component.translatable("buildcraft.gui.builder.template"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isIn(mouseX, mouseY, EXCAVATE_X, EXCAVATE_Y)) {
            menu.setExcavate(!tile().canExcavate());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
