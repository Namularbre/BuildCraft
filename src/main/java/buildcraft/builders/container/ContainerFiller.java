/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.container;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.items.SlotItemHandler;

import buildcraft.api.core.BCLog;
import buildcraft.api.filler.IFillerPattern;
import buildcraft.api.statements.IStatementParameter;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.statement.StatementTypeParam;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.filler.FillerType;
import buildcraft.builders.tile.TileFiller;

public class ContainerFiller extends ContainerBCTile<TileFiller> {
    protected static final IdAllocator IDS = ContainerBCTile.IDS.makeChild("filler");
    public static final int ID_PATTERN = IDS.allocId("PATTERN");
    public static final int ID_EXCAVATE = IDS.allocId("EXCAVATE");
    public static final int ID_INVERT = IDS.allocId("INVERT");

    public ContainerFiller(int id, Player player, TileFiller tile) {
        super(BCBuildersMenus.FILLER.get(), id, player, tile);
        addFullPlayerInventory(153);
        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 9; sx++) {
                addSlot(new SlotItemHandler(tile.invResources, sx + sy * 9, 8 + sx * 18, 85 + sy * 18));
            }
        }
    }

    /** The client-side constructor. */
    public ContainerFiller(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, readTile(inventory, buffer, TileFiller.class));
    }

    @Override
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    // Client -> server

    /** Changes the pattern on the client (so that the GUI reacts straight away) and on the server. */
    public void setPattern(@Nullable IFillerPattern pattern, IStatementParameter[] params) {
        FullStatement<IFillerPattern> full = tile.patternStatement;
        full.set(pattern, params);
        sendMessage(ID_PATTERN, full::writeToBuffer);
    }

    public void setExcavate(boolean value) {
        tile.setCanExcavate(value);
        sendMessage(ID_EXCAVATE, buffer -> buffer.writeBoolean(value));
    }

    public void setInverted(boolean value) {
        tile.setInverted(value);
        sendMessage(ID_INVERT, buffer -> buffer.writeBoolean(value));
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, @Nullable ServerPlayer sender) {
        super.readMessage(id, buffer, side, sender);
        if (side != LogicalSide.SERVER) {
            return;
        }
        if (id == ID_EXCAVATE) {
            tile.setCanExcavate(buffer.readBoolean());
        } else if (id == ID_INVERT) {
            tile.setInverted(buffer.readBoolean());
        } else if (id == ID_PATTERN) {
            try {
                IFillerPattern pattern = null;
                IStatementParameter[] params = new IStatementParameter[tile.patternStatement.getParamCount()];
                if (buffer.readBoolean()) {
                    pattern = FillerType.INSTANCE.readFromBuffer(buffer);
                    for (int p = 0; p < params.length; p++) {
                        params[p] = StatementTypeParam.INSTANCE.readFromBuffer(buffer);
                    }
                }
                tile.setPatternFromGui(pattern, params);
            } catch (RuntimeException e) {
                BCLog.logger.warn("[builders.filler] Ignored a bad pattern change: {}", e.toString());
            }
        }
    }
}
