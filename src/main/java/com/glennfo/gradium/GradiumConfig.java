package com.glennfo.gradium;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GradiumConfig {

    private static final String FILE_NAME = "gradium.properties";

    private static int minSavesPerTick = 1;
    private static int maxSavesPerTick = 5;

    /** Minimum POI/data/player saves per tick, used when server TPS is low. */
    public static int minSavesPerTick() {
        return minSavesPerTick;
    }

    /** Maximum POI/data/player saves per tick, used when server TPS is healthy. */
    public static int maxSavesPerTick() {
        return maxSavesPerTick;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                props.load(r);
            } catch (IOException e) {
                System.err.println("[Gradium] Failed to read " + FILE_NAME + ", using defaults: " + e.getMessage());
            }
        }
        minSavesPerTick = parse(props.getProperty("minSavesPerTick"), 1, 1, 20);
        maxSavesPerTick = Math.max(minSavesPerTick, parse(props.getProperty("maxSavesPerTick"), 5, 1, 50));
        props.setProperty("minSavesPerTick", Integer.toString(minSavesPerTick));
        props.setProperty("maxSavesPerTick", Integer.toString(maxSavesPerTick));
        try (Writer w = Files.newBufferedWriter(file)) {
            props.store(w, "Gradium - saves per tick: minSavesPerTick (1-20, default 1) applies at low TPS, maxSavesPerTick (1-50, default 5) at healthy TPS.");
        } catch (IOException e) {
            System.err.println("[Gradium] Failed to write " + FILE_NAME + ": " + e.getMessage());
        }
    }

    private static int parse(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
