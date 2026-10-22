package com.glennfo.gradium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;

public class Gradium implements ModInitializer {

    public static final String MOD_ID = "gradium";

    private static volatile MinecraftServer currentServer;

    /**
     * True while the server is shutting down (or gone). Saves then run immediately instead
     * of being queued, so closing the world always writes everything.
     */
    public static boolean isShuttingDown() {
        MinecraftServer server = currentServer;
        return server == null || !server.isRunning();
    }

    @Override
    public void onInitialize() {
        GradiumConfig.load();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> currentServer = null);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TpsMonitor.recordTick();
            int rate = TpsMonitor.getAdaptiveRate(GradiumConfig.minSavesPerTick(), GradiumConfig.maxSavesPerTick());
            PoiSaveQueue.tick(rate);
            SavedDataQueue.tick(rate);
            PlayerSaveQueue.tick(rate);
        });

        // Flush everything still queued before vanilla runs its final shutdown save.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> drainAll());
        ServerWorldEvents.UNLOAD.register((server, level) -> drainAll());
    }

    private static void drainAll() {
        PoiSaveQueue.drainAll();
        SavedDataQueue.drainAll();
        PlayerSaveQueue.drainAll();
    }
}
