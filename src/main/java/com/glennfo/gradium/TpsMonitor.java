package com.glennfo.gradium;

/**
 * Rolling 20-tick TPS monitor. Records the real-time delta of each server tick
 * and exposes a smoothed TPS value used to scale save rates adaptively.
 */
public class TpsMonitor {

    private static final int WINDOW = 20;

    /** Nanosecond timestamps of the last WINDOW tick starts. */
    private static final long[] tickTimes = new long[WINDOW];
    private static int head = 0;
    private static int count = 0;
    private static long lastTickNanos = 0;

    /**
     * Call once per server tick (Phase.END) to record that a tick just finished.
     */
    public static void recordTick() {
        long now = System.nanoTime();
        if (lastTickNanos != 0) {
            long delta = now - lastTickNanos;
            tickTimes[head] = delta;
            head = (head + 1) % WINDOW;
            if (count < WINDOW) count++;
        }
        lastTickNanos = now;
    }

    /**
     * Returns smoothed TPS in the range [0, 20].
     * Returns 20.0 if fewer than 2 ticks have been recorded.
     */
    public static double getTps() {
        if (count < 2) return 20.0;
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += tickTimes[i];
        }
        double avgMillis = (sum / (double) count) / 1_000_000.0;
        // 50 ms = 20 TPS; longer avg = lower TPS
        double tps = 1000.0 / avgMillis;
        return Math.min(20.0, Math.max(0.0, tps));
    }

    /**
     * Linearly scales between minRate and maxRate based on current TPS.
     * At 20 TPS returns maxRate; at 0 TPS returns minRate.
     */
    public static int getAdaptiveRate(int minRate, int maxRate) {
        double tps = getTps();
        double fraction = tps / 20.0;
        int rate = (int) Math.round(minRate + fraction * (maxRate - minRate));
        return Math.max(minRate, Math.min(maxRate, rate));
    }
}
