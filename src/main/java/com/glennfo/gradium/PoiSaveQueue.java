package com.glennfo.gradium;

import net.minecraft.world.entity.ai.village.poi.PoiManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Queues {@link PoiManager} instances for deferred tick/save calls.
 *
 * Intercept strategy: PoiManager overrides {@code tick(BooleanSupplier)} from SectionStorage,
 * which is the method the server calls during each auto-save cycle to flush dirty POI sections.
 * We intercept that method and spread the work across ticks.
 *
 * When {@code processing} is true the mixin lets calls through so we don't recurse when
 * we deliberately drain the queue.
 */
public class PoiSaveQueue {

    /** Set to true while we are deliberately calling tick() so the mixin passes through. */
    public static volatile boolean processing = false;

    private static final Deque<PoiManager> queue = new ArrayDeque<>();
    private static final Set<PoiManager> queued = new HashSet<>();

    /** Enqueue a PoiManager for deferred saving. Returns false if already enqueued (dedup). */
    public static synchronized boolean enqueue(PoiManager manager) {
        if (!queued.add(manager)) return false;
        queue.addLast(manager);
        return true;
    }

    /**
     * Process up to {@code n} PoiManagers from the front of the queue.
     * Called once per server tick. Passes {@code () -> true} so the underlying
     * SectionStorage tick flushes everything that is dirty.
     */
    public static synchronized void tick(int n) {
        if (queue.isEmpty()) return;
        processing = true;
        try {
            int remaining = n;
            while (remaining > 0 && !queue.isEmpty()) {
                PoiManager manager = queue.pollFirst();
                queued.remove(manager);
                try {
                    manager.tick(() -> true);
                } catch (Exception e) {
                    System.err.println("[Gradium] PoiSaveQueue: tick failed: " + e.getMessage());
                }
                remaining--;
            }
        } finally {
            processing = false;
        }
    }

    /**
     * Immediately drain the entire queue. Called before level unload or server shutdown.
     */
    public static synchronized void drainAll() {
        processing = true;
        try {
            while (!queue.isEmpty()) {
                PoiManager manager = queue.pollFirst();
                queued.remove(manager);
                try {
                    manager.tick(() -> true);
                } catch (Exception e) {
                    System.err.println("[Gradium] PoiSaveQueue: drainAll tick failed: " + e.getMessage());
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
