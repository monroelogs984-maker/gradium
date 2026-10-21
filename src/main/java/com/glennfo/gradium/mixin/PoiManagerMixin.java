package com.glennfo.gradium.mixin;

import com.glennfo.gradium.Gradium;
import com.glennfo.gradium.PoiSaveQueue;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin {

    /**
     * Intercept PoiManager.tick(BooleanSupplier) at HEAD.
     * This is the method the server calls during its auto-save cycle to flush dirty POI sections.
     * When processing is false (normal game flow) we cancel and enqueue for deferred handling.
     * When processing is true, PoiSaveQueue is deliberately draining and we let it through.
     *
     * Note: PoiManager.tick() is also called during chunk loading for POI lookups via
     * ensureLoadedAndValid. In those paths the hasWork() check inside SectionStorage.tick()
     * gates actual I/O, so intercepting all calls is safe — the worst case is we briefly
     * defer a POI flush by one Gradium tick cycle (typically < 5 ticks at default rates).
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (PoiSaveQueue.processing || Gradium.isShuttingDown()) {
            // Draining the queue or shutting down — let the real tick run.
            return;
        }
        // Defer this manager to the queue and suppress the vanilla call.
        PoiSaveQueue.enqueue((PoiManager) (Object) this);
        ci.cancel();
    }
}
