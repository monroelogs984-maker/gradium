package com.glennfo.gradium;

import com.glennfo.gradium.mixin.DimensionSavedDataManagerAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class SavedDataQueue {

    public static class Entry {
        public final DimensionDataStorage storage;
        public final String id;
        public final SavedData data;
        Entry(DimensionDataStorage storage, String id, SavedData data) {
            this.storage = storage;
            this.id = id;
            this.data = data;
        }
    }

    public static volatile boolean processing = false;

    private static final Deque<Entry> queue = new ArrayDeque<Entry>();
    private static final Set<String> queued = new HashSet<String>();

    private static String dedupKey(DimensionDataStorage storage, String id) {
        return System.identityHashCode(storage) + ":" + id;
    }

    public static synchronized boolean enqueue(DimensionDataStorage storage, String id, SavedData data) {
        String key = dedupKey(storage, id);
        if (!queued.add(key)) return false;
        queue.addLast(new Entry(storage, id, data));
        return true;
    }

    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(dedupKey(entry.storage, entry.id));
                saveEntry(entry);
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(dedupKey(entry.storage, entry.id));
                saveEntry(entry);
            }
        } finally {
            processing = false;
            queue.clear();
            queued.clear();
        }
    }

    public static synchronized void clear() {
        queue.clear();
        queued.clear();
    }

    public static synchronized int size() {
        return queue.size();
    }

    private static void saveEntry(Entry entry) {
        if (!entry.data.isDirty()) return;
        try {
            File dataFolder = ((DimensionSavedDataManagerAccessor) entry.storage).getDataFolder();
            File file = new File(dataFolder, entry.id + ".dat");
            entry.data.save(file);
        } catch (Exception e) {
            System.err.println("[Gradium] SavedDataQueue: save failed for '" + entry.id + "': " + e.getMessage());
        }
    }
}
