package com.glennfo.gradium;

import net.minecraft.village.PointOfInterestManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class PoiSaveQueue {

    public static volatile boolean processing = false;

    private static final Deque<PointOfInterestManager> queue = new ArrayDeque<PointOfInterestManager>();
    private static final Set<PointOfInterestManager> queued = new HashSet<PointOfInterestManager>();

    public static synchronized boolean enqueue(PointOfInterestManager manager) {
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
                PointOfInterestManager manager = queue.pollFirst();
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
                PointOfInterestManager manager = queue.pollFirst();
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
