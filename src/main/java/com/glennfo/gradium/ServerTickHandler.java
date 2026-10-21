package com.glennfo.gradium;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Gradium.MOD_ID)
public class ServerTickHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TpsMonitor.recordTick();

        int rate = TpsMonitor.getAdaptiveRate(
            GradiumConfig.MIN_SAVES_PER_TICK.get(),
            GradiumConfig.MAX_SAVES_PER_TICK.get()
        );

        PoiSaveQueue.tick(rate);
        SavedDataQueue.tick(rate);
        PlayerSaveQueue.tick(rate);
    }

    /** Flush everything still queued before vanilla runs its final shutdown save. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PoiSaveQueue.drainAll();
        SavedDataQueue.drainAll();
        PlayerSaveQueue.drainAll();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        PoiSaveQueue.drainAll();
        SavedDataQueue.drainAll();
        PlayerSaveQueue.drainAll();
    }
}
