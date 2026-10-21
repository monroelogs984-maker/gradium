package com.glennfo.gradium;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(Gradium.MOD_ID)
public class Gradium {

    public static final String MOD_ID = "gradium";

    /**
     * True while the server is shutting down (or gone). Saves then run immediately instead
     * of being queued, so closing the world always writes everything.
     */
    public static boolean isShuttingDown() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null || !server.isRunning();
    }

    public Gradium(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, GradiumConfig.SPEC, "gradium-server.toml");
    }
}
