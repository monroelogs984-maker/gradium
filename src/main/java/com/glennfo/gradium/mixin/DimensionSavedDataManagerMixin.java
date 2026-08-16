package com.glennfo.gradium.mixin;

import com.glennfo.gradium.SavedDataQueue;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(DimensionSavedDataManager.class)
public abstract class DimensionSavedDataManagerMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSave(CallbackInfo ci) {
        if (SavedDataQueue.processing) return;

        Map<String, WorldSavedData> cache = ((DimensionSavedDataManagerAccessor) this).getCache();
        if (cache == null || cache.isEmpty()) return;

        DimensionSavedDataManager self = (DimensionSavedDataManager) (Object) this;

        for (Map.Entry<String, WorldSavedData> entry : cache.entrySet()) {
            WorldSavedData data = entry.getValue();
            if (data != null && data.isDirty()) {
                SavedDataQueue.enqueue(self, entry.getKey(), data);
            }
        }

        ci.cancel();
    }
}
