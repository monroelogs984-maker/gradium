package com.glennfo.gradium.mixin;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;
import java.util.Map;

@Mixin(DimensionDataStorage.class)
public interface DimensionSavedDataManagerAccessor {

    @Accessor("cache")
    Map<String, SavedData> getCache();

    @Accessor("dataFolder")
    File getDataFolder();
}
