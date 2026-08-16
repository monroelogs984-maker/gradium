package com.glennfo.gradium;

import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Gradium.MOD_ID)
public class ServerTickHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TpsMonitor.recordTick();

        int rate = TpsMonitor.getAdaptiveRate(
            GradiumConfig.MIN_SAVES_PER_TICK.get(),
            GradiumConfig.MAX_SAVES_PER_TICK.get()
        );

        PoiSaveQueue.tick(rate);
        SavedDataQueue.tick(rate);
        PlayerSaveQueue.tick(rate);
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        PoiSaveQueue.drainAll();
        SavedDataQueue.drainAll();
        PlayerSaveQueue.drainAll();
    }
}
