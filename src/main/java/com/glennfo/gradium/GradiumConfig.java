package com.glennfo.gradium;

import net.minecraftforge.common.ForgeConfigSpec;

public class GradiumConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue MIN_SAVES_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_SAVES_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Gradium — adaptive entity & POI save spreading").push("general");

        MIN_SAVES_PER_TICK = builder
            .comment("Minimum entity/POI managers saved per tick (applied when server TPS is low).",
                     "Range: 1–20. Default: 1.")
            .defineInRange("minSavesPerTick", 1, 1, 20);

        MAX_SAVES_PER_TICK = builder
            .comment("Maximum entity/POI managers saved per tick (applied when server TPS is healthy).",
                     "Range: 1–50. Default: 5.")
            .defineInRange("maxSavesPerTick", 5, 1, 50);

        builder.pop();
        SPEC = builder.build();
    }
}
