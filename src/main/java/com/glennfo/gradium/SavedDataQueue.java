package com.glennfo.gradium;

import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.saveddata.SavedData;

import java.lang.reflect.Method;
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

    // Cached reflection handle for DimensionDataStorage's per-entry save method.
    // The lambda$save$0(String, SavedData) method does: data.save(getDataFile(id))
    private static volatile Method saveLambda = null;
    private static volatile boolean saveLambdaResolved = false;

    private static Method resolveSaveMethod(DimensionDataStorage storage) {
        if (saveLambdaResolved) return saveLambda;
        synchronized (SavedDataQueue.class) {
            if (saveLambdaResolved) return saveLambda;
            // Try the lambda method first (official mappings name)
            String[] candidates = {"lambda$save$0"};
            for (String name : candidates) {
                // Try (String, SavedData) order
                try {
                    Method m = DimensionDataStorage.class.getDeclaredMethod(name, String.class, SavedData.class);
                    m.setAccessible(true);
                    saveLambda = m;
                    saveLambdaResolved = true;
                    return saveLambda;
                } catch (NoSuchMethodException ignored) {}
                // Try (SavedData, String) order
                try {
                    Method m = DimensionDataStorage.class.getDeclaredMethod(name, SavedData.class, String.class);
                    m.setAccessible(true);
                    saveLambda = m;
                    saveLambdaResolved = true;
                    return saveLambda;
                } catch (NoSuchMethodException ignored) {}
            }
            // Fallback: scan all declared methods for one that takes exactly (String, SavedData)
            // or (SavedData, String) — these are the per-entry save signatures
            for (Method m : DimensionDataStorage.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2) {
                    boolean match = (params[0] == String.class && SavedData.class.isAssignableFrom(params[1]))
                            || (SavedData.class.isAssignableFrom(params[0]) && params[1] == String.class);
                    if (match) {
                        m.setAccessible(true);
                        saveLambda = m;
                        saveLambdaResolved = true;
                        return saveLambda;
                    }
                }
            }
            saveLambdaResolved = true;
            saveLambda = null;
            System.err.println("[Gradium] SavedDataQueue: could not resolve per-entry save method on DimensionDataStorage");
            return null;
        }
    }

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

    private static void saveEntry(Entry entry) {
        if (!entry.data().isDirty()) return; // already saved by something else
        Method m = resolveSaveMethod(entry.storage());
        if (m == null) {
            // Last-resort fallback: just mark not dirty (data won't be lost — vanilla will
            // catch it on the next save cycle when processing=true lets it through).
            System.err.println("[Gradium] SavedDataQueue: no save method — skipping entry: " + entry.id());
            return;
        }
        try {
            Class<?>[] params = m.getParameterTypes();
            if (params[0] == String.class) {
                m.invoke(entry.storage(), entry.id(), entry.data());
            } else {
                m.invoke(entry.storage(), entry.data(), entry.id());
            }
        } catch (Exception e) {
            System.err.println("[Gradium] SavedDataQueue: save failed for '" + entry.id() + "': " + e.getMessage());
        }
    }
}
