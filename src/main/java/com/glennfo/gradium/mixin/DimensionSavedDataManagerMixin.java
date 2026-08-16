package com.glennfo.gradium.mixin;

import com.glennfo.gradium.SavedDataQueue;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(DimensionDataStorage.class)
public abstract class DimensionSavedDataManagerMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSave(CallbackInfo ci) {
        if (SavedDataQueue.processing) return;

        Map<String, SavedData> cache = ((DimensionSavedDataManagerAccessor) this).getCache();
        if (cache == null || cache.isEmpty()) return;

        DimensionDataStorage self = (DimensionDataStorage) (Object) this;

        for (Map.Entry<String, SavedData> entry : cache.entrySet()) {
            SavedData data = entry.getValue();
            if (data != null && data.isDirty()) {
                SavedDataQueue.enqueue(self, entry.getKey(), data);
            }
        }

        ci.cancel();
    }
}
