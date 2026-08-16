package com.glennfo.gradium.mixin;

import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;
import java.util.Map;

@Mixin(DimensionSavedDataManager.class)
public interface DimensionSavedDataManagerAccessor {

    @Accessor("cache")
    Map<String, WorldSavedData> getCache();

    @Accessor("dataFolder")
    File getDataFolder();
}
