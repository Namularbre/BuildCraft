/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;
import buildcraft.api.tiles.IControllable;

import buildcraft.lib.gui.GuiBC;
import buildcraft.lib.statement.FullStatement;

import buildcraft.BuildCraft;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.filler.BCBuildersStatements;
import buildcraft.builders.tile.TileFiller;
import buildcraft.core.statements.BCStatement;

/** The GUI of the filler. The patterns are listed on the left: clicking one sets it. The parameters are changed by
 * clicking them (right click goes the other way). */
public class GuiFiller extends GuiBC<ContainerFiller> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/filler.png");
    private static final int CELL = 18;
    private static final int PANEL_COLUMNS = 4;
    private static final int PATTERN_X = 12, PATTERN_Y = 32, PATTERN_SIZE = 32;
    private static final int PARAM_X = 53, PARAM_Y = 39;
    private static final int EXCAVATE_X = 130, INVERT_X = 152, BUTTON_Y = 40;

    public GuiFiller(ContainerFiller container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 176, 241);
        inventoryLabelX = 7;
        inventoryLabelY = 141;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        titleLabelY = 6;
    }

    private TileFiller tile() {
        return menu.tile;
    }

    private FullStatement<IFillerPattern> pattern() {
        return tile().patternStatement;
    }

    // Layout

    private int panelColumns() {
        return Math.max(1, Math.min(PANEL_COLUMNS, (leftPos - 10) / CELL));
    }

    private int panelX() {
        return leftPos - (panelColumns() * CELL + 8) - 2;
    }

    private record PanelEntry(IFillerPattern pattern, int x, int y) {}

    private List<PanelEntry> panelEntries() {
        List<PanelEntry> entries = new ArrayList<>();
        int columns = panelColumns();
        int x0 = panelX() + 4;
        IFillerPattern[] patterns = BCBuildersStatements.PATTERNS;
        for (int i = 0; i < patterns.length; i++) {
            entries.add(new PanelEntry(patterns[i], x0 + (i % columns) * CELL, topPos + 4 + (i / columns) * CELL));
        }
        return entries;
    }

    private static boolean isIn(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        IFillerPattern current = pattern().get();
        if (current != null && current.getSprite() != null) {
            graphics.blit(current.getSprite(), leftPos + PATTERN_X, topPos + PATTERN_Y, PATTERN_SIZE, PATTERN_SIZE, 0,
                0, 16, 16, 16, 16);
        }
        int maxParams = current == null ? 0 : current.maxParameters();
        for (int i = 0; i < pattern().getParamCount(); i++) {
            int x = leftPos + PARAM_X + i * CELL;
            int y = topPos + PARAM_Y;
            IStatementParameter param = pattern().get(i);
            if (i >= maxParams) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80_00_00_00);
            } else if (param != null && param.getSprite() != null) {
                graphics.blit(param.getSprite(), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
            }
        }
        drawButton(graphics, EXCAVATE_X, 192, tile().canExcavate(), mouseX, mouseY);
        drawButton(graphics, INVERT_X, 224, tile().isInverted(), mouseX, mouseY);
        if (tile().getControlMode() != IControllable.Mode.ON) {
            String mode = tile().getControlMode().lowerCaseName;
            ResourceLocation sprite = BCStatement.sprite("action_machinecontrol_" + mode);
            graphics.blit(sprite, leftPos + 50, topPos + 14, 0, 0, 16, 16, 16, 16);
        }

        // The patterns
        List<PanelEntry> entries = panelEntries();
        int x = panelX();
        int width = panelColumns() * CELL + 8;
        int bottom = entries.get(entries.size() - 1).y + CELL + 4;
        graphics.fill(x, topPos, x + width, bottom, 0xFF_C6_C6_C6);
        graphics.renderOutline(x, topPos, width, bottom - topPos, 0xFF_55_55_55);
        for (PanelEntry entry : entries) {
            graphics.fill(entry.x, entry.y, entry.x + CELL, entry.y + CELL, 0xFF_37_37_37);
            graphics.fill(entry.x + 1, entry.y + 1, entry.x + CELL, entry.y + CELL, 0xFF_FF_FF_FF);
            int colour = entry.pattern == current ? 0xFF_6C_D4_1F : 0xFF_8B_8B_8B;
            graphics.fill(entry.x + 1, entry.y + 1, entry.x + CELL - 1, entry.y + CELL - 1, colour);
            if (entry.pattern.getSprite() != null) {
                graphics.blit(entry.pattern.getSprite(), entry.x + 1, entry.y + 1, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    private void drawButton(GuiGraphics graphics, int x, int u, boolean active, int mouseX, int mouseY) {
        boolean hovered = isIn(mouseX, mouseY, leftPos + x, topPos + BUTTON_Y, 16, 16);
        graphics.blit(TEXTURE, leftPos + x, topPos + BUTTON_Y, u + (active ? 16 : 0), hovered ? 16 : 0, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("buildcraft.gui.filler.resources"), 7, 74, 0x40_40_40, false);
        Component progress = tile().clientFinished ? Component.translatable("buildcraft.gui.filler.finished")
            : Component.translatable("buildcraft.gui.filler.progress", tile().getBuilder().leftToBreak,
                tile().getBuilder().leftToPlace);
        graphics.drawString(font, progress, PARAM_X, 62, 0x40_40_40, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<Component> tooltip = getTooltipAt(mouseX, mouseY);
        if (tooltip != null && !tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private List<Component> getTooltipAt(double mouseX, double mouseY) {
        for (PanelEntry entry : panelEntries()) {
            if (isIn(mouseX, mouseY, entry.x, entry.y, CELL, CELL)) {
                return List.of(entry.pattern.getDescription());
            }
        }
        if (isIn(mouseX, mouseY, leftPos + PATTERN_X, topPos + PATTERN_Y, PATTERN_SIZE, PATTERN_SIZE)) {
            List<Component> list = new ArrayList<>();
            IFillerPattern current = pattern().get();
            if (current != null) {
                list.add(current.getDescription());
            }
            if (tile().isLocked()) {
                list.add(Component.translatable("buildcraft.gui.filler.locked"));
            }
            return list;
        }
        for (int i = 0; i < pattern().getParamCount(); i++) {
            if (isIn(mouseX, mouseY, leftPos + PARAM_X + i * CELL, topPos + PARAM_Y, CELL, CELL)) {
                IStatementParameter param = pattern().get(i);
                if (param == null) {
                    return null;
                }
                List<Component> list = new ArrayList<>();
                for (Component line : param.getTooltip()) {
                    if (line != null) {
                        list.add(line);
                    }
                }
                return list;
            }
        }
        if (isIn(mouseX, mouseY, leftPos + EXCAVATE_X, topPos + BUTTON_Y, 16, 16)) {
            return List.of(Component.translatable("buildcraft.gui.filler.excavate." + tile().canExcavate()));
        }
        if (isIn(mouseX, mouseY, leftPos + INVERT_X, topPos + BUTTON_Y, 16, 16)) {
            return List.of(Component.translatable("buildcraft.gui.filler.invert." + tile().isInverted()));
        }
        return null;
    }

    // Interaction

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PanelEntry entry : panelEntries()) {
            if (isIn(mouseX, mouseY, entry.x, entry.y, CELL, CELL)) {
                menu.setPattern(entry.pattern, pattern().getParameters().clone());
                return true;
            }
        }
        if (isIn(mouseX, mouseY, leftPos + PATTERN_X, topPos + PATTERN_Y, PATTERN_SIZE, PATTERN_SIZE)) {
            if (button == 1) {
                menu.setPattern(BCBuildersStatements.PATTERN_NONE, new IStatementParameter[0]);
            }
            return true;
        }
        for (int i = 0; i < pattern().getParamCount(); i++) {
            if (isIn(mouseX, mouseY, leftPos + PARAM_X + i * CELL, topPos + PARAM_Y, CELL, CELL)) {
                clickParameter(i, button);
                return true;
            }
        }
        if (isIn(mouseX, mouseY, leftPos + EXCAVATE_X, topPos + BUTTON_Y, 16, 16)) {
            menu.setExcavate(!tile().canExcavate());
            return true;
        }
        if (isIn(mouseX, mouseY, leftPos + INVERT_X, topPos + BUTTON_Y, 16, 16)) {
            menu.setInverted(!tile().isInverted());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickParameter(int index, int button) {
        IFillerPattern current = pattern().get();
        IStatementParameter param = pattern().get(index);
        if (current == null || param == null) {
            return;
        }
        IStatementParameter result = param.onClick(tile(), current, menu.getCarried(),
            new StatementMouseClick(button, hasShiftDown()));
        if (result == null) {
            IStatementParameter[] possible = param.getPossible(tile());
            if (possible != null) {
                List<IStatementParameter> values = new ArrayList<>();
                for (IStatementParameter value : possible) {
                    if (value != null) {
                        values.add(value);
                    }
                }
                if (!values.isEmpty()) {
                    int i = Math.max(0, values.indexOf(param));
                    result = values.get(Math.floorMod(i + (button == 0 ? 1 : -1), values.size()));
                }
            }
        }
        if (result != null) {
            IStatementParameter[] params = pattern().getParameters().clone();
            params[index] = result;
            menu.setPattern(current, params);
        }
    }
}
