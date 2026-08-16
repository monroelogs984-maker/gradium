package com.glennfo.gradium;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Gradium.MOD_ID)
public class Gradium {

    public static final String MOD_ID = "gradium";

    public Gradium() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, GradiumConfig.SPEC, "gradium-server.toml");
        MinecraftForge.EVENT_BUS.register(ServerTickHandler.class);
    }
}
