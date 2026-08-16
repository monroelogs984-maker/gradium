package com.glennfo.gradium;

import net.neoforged.neoforge.common.ModConfigSpec;

public class GradiumConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MIN_SAVES_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_SAVES_PER_TICK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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
