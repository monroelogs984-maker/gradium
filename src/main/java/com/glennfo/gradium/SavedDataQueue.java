package com.glennfo.gradium;

import com.glennfo.gradium.mixin.DimensionDataStorageAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Queues dirty {@link SavedData} entries from {@link DimensionDataStorage} for deferred saving.
 *
 * <p>The mixin intercepts {@code DimensionDataStorage.save()} and instead enqueues each dirty
 * entry here. We drain {@code n} entries per tick based on the adaptive rate. When
 * {@code processing} is true the mixin lets the real {@code save()} through so we don't
 * recurse infinitely while draining.</p>
 */
public class SavedDataQueue {

    public record Entry(DimensionDataStorage storage, String id, SavedData data) {}

    /** Set to true while we are deliberately saving entries so the mixin passes through. */
    public static volatile boolean processing = false;

    private static final Deque<Entry> queue = new ArrayDeque<>();

    /**
     * Dedup key: combines identity hash of the storage with the entry id.
     * This prevents the same (storage, id) pair from being enqueued twice.
     */
    private static final Set<String> queued = new HashSet<>();

    private static String dedupKey(DimensionDataStorage storage, String id) {
        return System.identityHashCode(storage) + ":" + id;
    }

    /** Enqueue a dirty SavedData entry for deferred saving. Dedup by (storage, id) pair. */
    public static synchronized boolean enqueue(DimensionDataStorage storage, String id, SavedData data) {
        String key = dedupKey(storage, id);
        if (!queued.add(key)) return false;
        queue.addLast(new Entry(storage, id, data));
        return true;
    }

    /** Process up to {@code n} entries from the front of the queue. Called once per server tick. */
    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(dedupKey(entry.storage(), entry.id()));
                saveEntry(entry);
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    /** Immediately drain the entire queue. Called before level unload or server shutdown. */
    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                Entry entry = queue.pollFirst();
                queued.remove(dedupKey(entry.storage(), entry.id()));
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

    /**
     * Writes one entry the same way vanilla's DimensionDataStorage.save() does. If that fails
     * for any reason, falls back to the vanilla save for the whole storage, so data is never
     * dropped.
     */
    private static void saveEntry(Entry entry) {
        if (!entry.data().isDirty()) return; // already saved by something else
        DimensionDataStorageAccessor accessor = (DimensionDataStorageAccessor) entry.storage();
        try {
            entry.data().save(accessor.invokeGetDataFile(entry.id()), accessor.getRegistries());
        } catch (Exception e) {
            System.err.println("[Gradium] SavedDataQueue: save failed for '" + entry.id()
                    + "', falling back to a full vanilla save: " + e.getMessage());
            entry.storage().save(); // processing is true here, so the mixin lets this through
        }
    }
}
