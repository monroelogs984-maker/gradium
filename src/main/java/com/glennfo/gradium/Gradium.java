package com.glennfo.gradium;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Gradium.MOD_ID)
public class Gradium {

    public static final String MOD_ID = "gradium";

    public Gradium(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, GradiumConfig.SPEC, "gradium-server.toml");
        NeoForge.EVENT_BUS.register(ServerTickHandler.class);
    }
}
