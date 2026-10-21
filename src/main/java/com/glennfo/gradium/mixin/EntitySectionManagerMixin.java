package com.glennfo.gradium.mixin;

import com.glennfo.gradium.EntitySaveQueue;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentEntitySectionManager.class)
public abstract class EntitySectionManagerMixin {

    /**
     * Intercept autoSave() at HEAD. When processing is false (normal game flow), we cancel
     * the call and enqueue this manager for deferred handling. When processing is true, the
     * EntitySaveQueue is deliberately draining and we let the call through.
     */
    @Inject(method = "autoSave", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void gradium$interceptAutoSave(CallbackInfo ci) {
        if (EntitySaveQueue.processing) {
            // Draining the queue or shutting down — let the real autoSave run.
            return;
        }
        // Defer this manager to the queue and suppress the vanilla call.
        EntitySaveQueue.enqueue((PersistentEntitySectionManager<?>) (Object) this);
        ci.cancel();
    }
}
