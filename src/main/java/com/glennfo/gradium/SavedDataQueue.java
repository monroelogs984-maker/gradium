package com.glennfo.gradium;

import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SavedDataQueue {

    public static class Entry {
        public final DimensionSavedDataManager storage;
        public final String id;
        public final WorldSavedData data;
        Entry(DimensionSavedDataManager storage, String id, WorldSavedData data) {
            this.storage = storage;
            this.id = id;
            this.data = data;
        }
    }

    public static volatile boolean processing = false;

    private static final Deque<Entry> queue = new ArrayDeque<Entry>();
    private static final Set<String> queued = new HashSet<String>();

    private static volatile Method saveLambda = null;
    private static volatile boolean saveLambdaResolved = false;

    private static Method resolveSaveMethod(DimensionSavedDataManager storage) {
        if (saveLambdaResolved) return saveLambda;
        synchronized (SavedDataQueue.class) {
            if (saveLambdaResolved) return saveLambda;
            String[] candidates = {"lambda$save$0"};
            for (String name : candidates) {
                try {
                    Method m = DimensionSavedDataManager.class.getDeclaredMethod(name, String.class, WorldSavedData.class);
                    m.setAccessible(true);
                    saveLambda = m;
                    saveLambdaResolved = true;
                    return saveLambda;
                } catch (NoSuchMethodException ignored) {}
                try {
                    Method m = DimensionSavedDataManager.class.getDeclaredMethod(name, WorldSavedData.class, String.class);
                    m.setAccessible(true);
                    saveLambda = m;
                    saveLambdaResolved = true;
                    return saveLambda;
                } catch (NoSuchMethodException ignored) {}
            }
            for (Method m : DimensionSavedDataManager.class.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2) {
                    boolean match = (params[0] == String.class && WorldSavedData.class.isAssignableFrom(params[1]))
                            || (WorldSavedData.class.isAssignableFrom(params[0]) && params[1] == String.class);
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
            System.err.println("[Gradium] SavedDataQueue: could not resolve per-entry save method on DimensionSavedDataManager");
            return null;
        }
    }

    private static String dedupKey(DimensionSavedDataManager storage, String id) {
        return System.identityHashCode(storage) + ":" + id;
    }

    public static synchronized boolean enqueue(DimensionSavedDataManager storage, String id, WorldSavedData data) {
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
        Method m = resolveSaveMethod(entry.storage);
        if (m == null) {
            System.err.println("[Gradium] SavedDataQueue: no save method — skipping entry: " + entry.id);
            return;
        }
        try {
            Class<?>[] params = m.getParameterTypes();
            if (params[0] == String.class) {
                m.invoke(entry.storage, entry.id, entry.data);
            } else {
                m.invoke(entry.storage, entry.data, entry.id);
            }
        } catch (Exception e) {
            System.err.println("[Gradium] SavedDataQueue: save failed for '" + entry.id + "': " + e.getMessage());
        }
    }
}
