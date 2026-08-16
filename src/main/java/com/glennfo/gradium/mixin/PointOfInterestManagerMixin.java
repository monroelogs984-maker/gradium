package com.glennfo.gradium.mixin;

import com.glennfo.gradium.PoiSaveQueue;
import net.minecraft.village.PointOfInterestManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(PointOfInterestManager.class)
public abstract class PointOfInterestManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (PoiSaveQueue.processing) return;
        PoiSaveQueue.enqueue((PointOfInterestManager) (Object) this);
        ci.cancel();
    }
}
