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
public abstract class DimensionDataStorageMixin {

    /**
     * Intercept {@code DimensionDataStorage.save()} at HEAD.
     *
     * <p>When {@code processing} is false (normal game flow), we cancel the all-at-once sweep
     * and instead enqueue each dirty entry individually for deferred saving. When
     * {@code processing} is true, {@link SavedDataQueue} is deliberately draining and we let
     * the call through.</p>
     */
    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSave(CallbackInfo ci) {
        if (SavedDataQueue.processing) {
            // We are draining the queue — let the real save() run for this entry.
            return;
        }

        // Access the cache map via the companion accessor interface.
        Map<String, SavedData> cache = ((DimensionDataStorageAccessor) this).getCache();
        if (cache == null || cache.isEmpty()) return;

        DimensionDataStorage self = (DimensionDataStorage) (Object) this;

        for (Map.Entry<String, SavedData> entry : cache.entrySet()) {
            SavedData data = entry.getValue();
            if (data != null && data.isDirty()) {
                SavedDataQueue.enqueue(self, entry.getKey(), data);
            }
        }

        // Suppress the vanilla all-at-once save; our queue will handle each entry.
        ci.cancel();
    }
}
