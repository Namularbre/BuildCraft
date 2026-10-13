/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.statements.IGuiSlot;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.IStatementParameter.DrawType;
import buildcraft.api.statements.StatementMouseClick;

import buildcraft.lib.gui.GuiBC;
import buildcraft.lib.statement.ActionWrapper;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.lib.statement.TriggerWrapper;

import buildcraft.BuildCraft;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.gate.GateLogic;

/** The GUI of a gate. The possible triggers are listed on the left, and the possible actions on the right: clicking
 * one picks it up, and clicking a slot puts it there. Right clicking a slot clears it. */
public class GuiGate extends GuiBC<ContainerGate> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(BuildCraft.MODID, "textures/gui/gate.png");
    private static final int TOP = 16;
    private static final int CELL = 18;
    private static final int PANEL_COLUMNS = 5;

    private final int rows;
    private final int triggerArgs, actionArgs;
    private final int pairWidth, pairStart;
    /** The statement that was picked from a panel (and follows the mouse), or null. */
    @Nullable
    private StatementWrapper held;

    public GuiGate(ContainerGate container, Inventory inventory, Component title) {
        super(container, inventory, title, TEXTURE, 176, TOP + container.getRowCount() * CELL + 101);
        GateLogic gate = container.getGate();
        rows = container.getRowCount();
        triggerArgs = gate.variant.numTriggerArgs();
        actionArgs = gate.variant.numActionArgs();
        pairWidth = CELL * (3 + triggerArgs + actionArgs);
        pairStart = (162 - (pairWidth + (gate.isSplitInTwo() ? pairWidth + CELL : 0))) / 2;
        inventoryLabelY = TOP + rows * CELL + 4;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        menu.requestValidStatements();
    }

    // Layout

    private int pairX(int pair) {
        return leftPos + pairStart + 7 + (pair / rows) * (CELL + pairWidth);
    }

    private int pairY(int pair) {
        return topPos + TOP + (pair % rows) * CELL;
    }

    private int triggerX(int pair, int param) {
        return pairX(pair) + CELL * (param + 1);
    }

    private int actionX(int pair, int param) {
        return pairX(pair) + CELL * (2 + triggerArgs) + CELL * (param + 1);
    }

    private int connectorX(int pair) {
        return pairX(pair) + CELL * (1 + triggerArgs);
    }

    /** @return The number of panel columns that fit on the left (or right) of the GUI. */
    private int panelColumns() {
        return Math.max(1, Math.min(PANEL_COLUMNS, (leftPos - 10) / CELL));
    }

    private int panelX(boolean actions) {
        int width = panelColumns() * CELL + 8;
        return actions ? leftPos + imageWidth + 2 : leftPos - width - 2;
    }

    /** @return The panel entries, with their position. */
    private List<PanelEntry> panelEntries(boolean actions) {
        List<PanelEntry> entries = new ArrayList<>();
        List<? extends List<? extends StatementWrapper>> groups =
            actions ? ContainerGate.group(menu.possibleActions) : ContainerGate.group(menu.possibleTriggers);
        int x0 = panelX(actions) + 4;
        int y = topPos + 4;
        int columns = panelColumns();
        for (List<? extends StatementWrapper> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                entries.add(new PanelEntry(group.get(i), x0 + (i % columns) * CELL, y + (i / columns) * CELL));
            }
            y += ((group.size() + columns - 1) / columns) * CELL + 3;
        }
        return entries;
    }

    private record PanelEntry(StatementWrapper statement, int x, int y) {}

    private static boolean isIn(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseY >= y && mouseX < x + size && mouseY < y + size;
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GateLogic gate = menu.getGate();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, TOP);
        for (int row = 0; row < rows; row++) {
            graphics.blit(TEXTURE, leftPos, topPos + TOP + row * CELL, 0, 23, imageWidth, CELL);
        }
        graphics.blit(TEXTURE, leftPos, topPos + TOP + rows * CELL, 0, 48, imageWidth, 101);

        for (int pair = 0; pair < gate.statements.length; pair++) {
            GateLogic.StatementPair statements = gate.statements[pair];
            int y = pairY(pair);
            drawStatement(graphics, pairX(pair), y, statements.trigger);
            for (int p = 0; p < triggerArgs; p++) {
                drawParameter(graphics, triggerX(pair, p), y, statements.trigger, p);
            }
            drawStatement(graphics, actionX(pair, -1), y, statements.action);
            for (int p = 0; p < actionArgs; p++) {
                drawParameter(graphics, actionX(pair, p), y, statements.action, p);
            }

            int cx = connectorX(pair);
            boolean actionOn = gate.actionOn[pair];
            graphics.blit(TEXTURE, cx, y, 176 + (gate.triggerOn[pair] ? 18 : 0), 18, 7, CELL);
            graphics.blit(TEXTURE, cx + 7, y, 180 + (actionOn ? 18 : 0), 18, 4, CELL);
            boolean lit = actionOn && statements.action.get() != null;
            graphics.blit(TEXTURE, cx + 11, y, 187 + (lit ? 18 : 0), 18, 7, CELL);

            if (hasConnectionBelow(pair)) {
                boolean connected = gate.connections[pair];
                int v = 36 + (connected ? 18 : 0);
                graphics.blit(TEXTURE, cx, y + 9, 176 + (actionOn ? 18 : 0), v, CELL, 9);
                graphics.blit(TEXTURE, cx, y + 18, 176 + (gate.actionOn[pair + 1] ? 18 : 0), v + 9, CELL, 9);
                if (isIn(mouseX, mouseY, cx, y + 9, CELL)) {
                    graphics.blit(TEXTURE, cx, y + 9, 194, 72, CELL, CELL);
                }
            }
        }

        drawPanel(graphics, false);
        drawPanel(graphics, true);
    }

    /** @return True if the given pair can be grouped with the one below it (in the same column). */
    private boolean hasConnectionBelow(int pair) {
        return pair % rows < rows - 1 && pair + 1 < menu.getGate().statements.length;
    }

    private void drawPanel(GuiGraphics graphics, boolean actions) {
        List<PanelEntry> entries = panelEntries(actions);
        int x = panelX(actions);
        int width = panelColumns() * CELL + 8;
        int bottom = topPos + 4 + CELL;
        for (PanelEntry entry : entries) {
            bottom = Math.max(bottom, entry.y + CELL);
        }
        graphics.fill(x, topPos, x + width, bottom + 4, 0xFF_C6_C6_C6);
        graphics.renderOutline(x, topPos, width, bottom + 4 - topPos, 0xFF_55_55_55);
        if (entries.isEmpty()) {
            graphics.drawString(font, "-", x + width / 2 - 2, topPos + 9, 0xFF_40_40_40, false);
        }
        for (PanelEntry entry : entries) {
            drawSlotBackground(graphics, entry.x, entry.y, true);
            drawStatementIcon(graphics, entry.x + 1, entry.y + 1, entry.statement);
        }
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y, boolean enabled) {
        graphics.fill(x, y, x + CELL, y + CELL, 0xFF_37_37_37);
        graphics.fill(x + 1, y + 1, x + CELL, y + CELL, 0xFF_FF_FF_FF);
        graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, enabled ? 0xFF_8B_8B_8B : 0xFF_5A_5A_5A);
    }

    private static int getSideColour(EnumPipePart part) {
        return switch (part) {
            case DOWN -> 0xFF_33_33_33;
            case UP -> 0xFF_CC_CC_CC;
            case NORTH -> 0xFF_33_55_CC;
            case SOUTH -> 0xFF_CC_33_33;
            case WEST -> 0xFF_33_AA_33;
            case EAST -> 0xFF_DD_AA_22;
            default -> 0;
        };
    }

    private void drawStatementIcon(GuiGraphics graphics, int x, int y, StatementWrapper statement) {
        if (statement.sourcePart != EnumPipePart.CENTER) {
            int colour = getSideColour(statement.sourcePart);
            graphics.fill(x - 1, y - 1, x + 17, y + 17, (colour & 0xFF_FF_FF) | 0x90_00_00_00);
        }
        ResourceLocation sprite = statement.getSprite();
        if (sprite != null) {
            graphics.blit(sprite, x, y, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawStatement(GuiGraphics graphics, int x, int y, FullStatement<?> full) {
        drawSlotBackground(graphics, x, y, true);
        if (full.get() instanceof StatementWrapper statement) {
            drawStatementIcon(graphics, x + 1, y + 1, statement);
        }
    }

    private void drawParameter(GuiGraphics graphics, int x, int y, FullStatement<?> full, int index) {
        boolean enabled = full.get() != null && index < full.get().maxParameters();
        drawSlotBackground(graphics, x, y, enabled);
        IStatementParameter param = full.get(index);
        if (!enabled || param == null) {
            return;
        }
        DrawType type = param.getDrawType();
        boolean sprite = type != DrawType.STACK_ONLY && type != DrawType.STACK_ONLY_OR_QUESTION_MARK;
        boolean questionMark = type == DrawType.STACK_ONLY_OR_QUESTION_MARK
            || type == DrawType.SPRITE_STACK_OR_QUESTION_MARK || type == DrawType.STACK_OR_QUESTION_MARK_THEN_SPRITE;
        boolean stack = type != DrawType.SPRITE_ONLY;
        if (sprite && param.getSprite() != null) {
            graphics.blit(param.getSprite(), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        }
        ItemStack item = param.getItemStack();
        if (stack && !item.isEmpty()) {
            graphics.renderItem(item, x + 1, y + 1);
        } else if (stack && questionMark) {
            graphics.drawString(font, "?", x + 7, y + 5, 0xFF_FF_FF_FF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (held != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            drawStatementIcon(graphics, mouseX - 8, mouseY - 8, held);
            graphics.pose().popPose();
        } else {
            List<Component> tooltip = getTooltipAt(mouseX, mouseY);
            if (tooltip != null && !tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Nullable
    private List<Component> getTooltipAt(double mouseX, double mouseY) {
        for (boolean actions : new boolean[] { false, true }) {
            for (PanelEntry entry : panelEntries(actions)) {
                if (isIn(mouseX, mouseY, entry.x, entry.y, CELL)) {
                    return entry.statement.getTooltip();
                }
            }
        }
        GateLogic gate = menu.getGate();
        for (int pair = 0; pair < gate.statements.length; pair++) {
            int y = pairY(pair);
            GateLogic.StatementPair statements = gate.statements[pair];
            if (isIn(mouseX, mouseY, pairX(pair), y, CELL)) {
                return tooltipOf(statements.trigger.get());
            }
            if (isIn(mouseX, mouseY, actionX(pair, -1), y, CELL)) {
                return tooltipOf(statements.action.get());
            }
            for (int p = 0; p < triggerArgs; p++) {
                if (isIn(mouseX, mouseY, triggerX(pair, p), y, CELL)) {
                    return tooltipOf(statements.trigger.get(p));
                }
            }
            for (int p = 0; p < actionArgs; p++) {
                if (isIn(mouseX, mouseY, actionX(pair, p), y, CELL)) {
                    return tooltipOf(statements.action.get(p));
                }
            }
            if (hasConnectionBelow(pair) && isIn(mouseX, mouseY, connectorX(pair), y + 9, CELL)) {
                return List.of(Component.translatable("buildcraft.gate.connection"));
            }
        }
        return null;
    }

    @Nullable
    private static List<Component> tooltipOf(@Nullable IGuiSlot slot) {
        if (slot == null) {
            return null;
        }
        List<Component> tooltip = new ArrayList<>();
        for (Component line : slot.getTooltip()) {
            if (line != null) {
                tooltip.add(line);
            }
        }
        return tooltip;
    }

    // Interaction

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (boolean actions : new boolean[] { false, true }) {
            for (PanelEntry entry : panelEntries(actions)) {
                if (isIn(mouseX, mouseY, entry.x, entry.y, CELL)) {
                    held = button == 0 ? entry.statement : null;
                    return true;
                }
            }
        }
        GateLogic gate = menu.getGate();
        for (int pair = 0; pair < gate.statements.length; pair++) {
            int y = pairY(pair);
            if (isIn(mouseX, mouseY, pairX(pair), y, CELL)) {
                clickStatement(pair, false, button);
                return true;
            }
            if (isIn(mouseX, mouseY, actionX(pair, -1), y, CELL)) {
                clickStatement(pair, true, button);
                return true;
            }
            for (int p = 0; p < triggerArgs; p++) {
                if (isIn(mouseX, mouseY, triggerX(pair, p), y, CELL)) {
                    clickParameter(pair, false, p, button);
                    return true;
                }
            }
            for (int p = 0; p < actionArgs; p++) {
                if (isIn(mouseX, mouseY, actionX(pair, p), y, CELL)) {
                    clickParameter(pair, true, p, button);
                    return true;
                }
            }
            if (hasConnectionBelow(pair) && isIn(mouseX, mouseY, connectorX(pair), y + 9, CELL)) {
                menu.setConnected(pair, !gate.connections[pair]);
                return true;
            }
        }
        if (held != null) {
            held = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickStatement(int pair, boolean isAction, int button) {
        GateLogic.StatementPair statements = menu.getGate().statements[pair];
        if (button == 1) {
            // Right click clears the slot
            if (isAction) {
                menu.setAction(pair, null, new IStatementParameter[0]);
            } else {
                menu.setTrigger(pair, null, new IStatementParameter[0]);
            }
            held = null;
            return;
        }
        if (held == null) {
            return;
        }
        if (isAction && held instanceof ActionWrapper action) {
            menu.setAction(pair, action, statements.action.getParameters().clone());
            held = null;
        } else if (!isAction && held instanceof TriggerWrapper trigger) {
            menu.setTrigger(pair, trigger, statements.trigger.getParameters().clone());
            held = null;
        }
    }

    private void clickParameter(int pair, boolean isAction, int index, int button) {
        GateLogic gate = menu.getGate();
        GateLogic.StatementPair statements = gate.statements[pair];
        FullStatement<?> full = isAction ? statements.action : statements.trigger;
        IStatementParameter param = full.get(index);
        if (full.get() == null || param == null) {
            return;
        }
        StatementMouseClick click = new StatementMouseClick(button, hasShiftDown());
        IStatementParameter result = param.onClick(gate, full.get(), menu.getCarried(), click);
        if (result == null) {
            result = cycle(param, param.getPossible(gate), button == 0 ? 1 : -1);
        }
        if (result == null) {
            return;
        }
        IStatementParameter[] params = full.getParameters().clone();
        params[index] = result;
        if (isAction) {
            menu.setAction(pair, statements.action.get(), params);
        } else {
            menu.setTrigger(pair, statements.trigger.get(), params);
        }
    }

    /** @return The value after (or before) the current one in the possible values, or null if there aren't any. */
    @Nullable
    private static IStatementParameter cycle(IStatementParameter current, @Nullable IStatementParameter[] possible,
        int step) {
        if (possible == null) {
            return null;
        }
        List<IStatementParameter> values = new ArrayList<>();
        for (IStatementParameter value : possible) {
            if (value != null) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        int index = Math.max(0, values.indexOf(current));
        return values.get(Math.floorMod(index + step, values.size()));
    }
}
