package com.glennfo.gradium;

import net.minecraft.world.entity.ai.village.poi.PoiManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class PoiSaveQueue {

    public static volatile boolean processing = false;

    private static final Deque<PoiManager> queue = new ArrayDeque<PoiManager>();
    private static final Set<PoiManager> queued = new HashSet<PoiManager>();

    public static synchronized boolean enqueue(PoiManager manager) {
        if (!queued.add(manager)) return false;
        queue.addLast(manager);
        return true;
    }

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
