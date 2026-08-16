package com.glennfo.gradium;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Queues {@link PersistentEntitySectionManager} instances for deferred autoSave calls.
 * The mixin intercepts autoSave() and routes managers through here; we drain N per tick
 * based on the adaptive rate. When {@code processing} is true the mixin lets calls through
 * so we don't recurse infinitely when we deliberately drain the queue.
 */
public class EntitySaveQueue {

    /** Set to true while we are deliberately calling autoSave() so the mixin passes through. */
    public static volatile boolean processing = false;

    private static final Deque<PersistentEntitySectionManager<?>> queue = new ArrayDeque<>();
    private static final Set<PersistentEntitySectionManager<?>> queued = new HashSet<>();

    /** Enqueue a manager for deferred saving. Returns false if already enqueued (dedup). */
    public static synchronized boolean enqueue(PersistentEntitySectionManager<?> manager) {
        if (!queued.add(manager)) return false;
        queue.addLast(manager);
        return true;
    }

    /**
     * Process up to {@code n} managers from the front of the queue.
     * Called once per server tick.
     */
    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                PersistentEntitySectionManager<?> manager = queue.pollFirst();
                queued.remove(manager);
                try {
                    manager.autoSave();
                } catch (Exception e) {
                    System.err.println("[Gradium] EntitySaveQueue: autoSave failed: " + e.getMessage());
                }
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    /**
     * Immediately drain the entire queue. Called before level unload or server shutdown
     * so nothing is lost.
     */
    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                PersistentEntitySectionManager<?> manager = queue.pollFirst();
                queued.remove(manager);
                try {
                    manager.autoSave();
                } catch (Exception e) {
                    System.err.println("[Gradium] EntitySaveQueue: drainAll autoSave failed: " + e.getMessage());
                }
            }
        } finally {
            processing = false;
            queue.clear();
            queued.clear();
        }
    }

    public static synchronized int size() {
        return queue.size();
    }
}
