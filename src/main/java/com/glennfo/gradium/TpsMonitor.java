package com.glennfo.gradium;

public class TpsMonitor {

    private static final int WINDOW = 20;

    private static final long[] tickTimes = new long[WINDOW];
    private static int head = 0;
    private static int count = 0;
    private static long lastTickNanos = 0;

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

    public static double getTps() {
        if (count < 2) return 20.0;
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += tickTimes[i];
        }
        double avgMillis = (sum / (double) count) / 1_000_000.0;
        double tps = 1000.0 / avgMillis;
        return Math.min(20.0, Math.max(0.0, tps));
    }

    public static int getAdaptiveRate(int minRate, int maxRate) {
        double tps = getTps();
        double fraction = tps / 20.0;
        int rate = (int) Math.round(minRate + fraction * (maxRate - minRate));
        return Math.max(minRate, Math.min(maxRate, rate));
    }
}
