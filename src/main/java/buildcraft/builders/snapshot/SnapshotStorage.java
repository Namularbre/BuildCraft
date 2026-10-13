/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import buildcraft.api.core.BCLog;

/** Every snapshot of the world, by its key. Items only hold the key, as snapshots can be big. */
public class SnapshotStorage extends SavedData {
    private static final String NAME = "buildcraft_snapshots";

    private final Map<String, CompoundTag> snapshots = new HashMap<>();
    private final Map<String, Snapshot> cache = new HashMap<>();

    public static SnapshotStorage get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SnapshotStorage::load, SnapshotStorage::new,
            NAME);
    }

    private static SnapshotStorage load(CompoundTag nbt) {
        SnapshotStorage storage = new SnapshotStorage();
        CompoundTag all = nbt.getCompound("snapshots");
        for (String key : all.getAllKeys()) {
            storage.snapshots.put(key, all.getCompound(key));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag all = new CompoundTag();
        snapshots.forEach(all::put);
        nbt.put("snapshots", all);
        return nbt;
    }

    /** @return The key of the snapshot (it is stored if it wasn't already). */
    public String add(Snapshot snapshot) {
        String key = snapshot.computeKey();
        if (!snapshots.containsKey(key)) {
            snapshots.put(key, Snapshot.write(snapshot));
            setDirty();
        }
        cache.put(key, snapshot);
        return key;
    }

    @Nullable
    public Snapshot get(String key) {
        Snapshot snapshot = cache.get(key);
        if (snapshot == null) {
            CompoundTag nbt = snapshots.get(key);
            if (nbt == null) {
                return null;
            }
            try {
                snapshot = Snapshot.read(nbt);
            } catch (RuntimeException e) {
                BCLog.logger.warn("[builders.snapshot] Couldn't read the snapshot {}: {}", key, e.toString());
                return null;
            }
            cache.put(key, snapshot);
        }
        return snapshot;
    }
}
