package com.glennfo.gradium.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.File;
import java.util.Map;

@Mixin(DimensionDataStorage.class)
public interface DimensionDataStorageAccessor {

    @Accessor("cache")
    Map<String, SavedData> getCache();

    @Accessor("registries")
    HolderLookup.Provider getRegistries();

    @Invoker("getDataFile")
    File invokeGetDataFile(String id);
}
