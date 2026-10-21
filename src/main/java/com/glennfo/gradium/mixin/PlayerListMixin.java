package com.glennfo.gradium.mixin;

import com.glennfo.gradium.Gradium;
import com.glennfo.gradium.PlayerSaveQueue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    private List<ServerPlayer> players;

    /**
     * Intercept {@code PlayerList.saveAll()} at HEAD.
     *
     * <p>When {@code processing} is false (normal game flow), we cancel the all-at-once write
     * and instead enqueue each online player for deferred saving. When {@code processing} is
     * true, {@link PlayerSaveQueue} is deliberately draining and we let the call through.</p>
     */
    @Inject(method = "saveAll", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSaveAll(CallbackInfo ci) {
        if (PlayerSaveQueue.processing || Gradium.isShuttingDown()) {
            // Draining the queue or shutting down — let the real saveAll() run.
            return;
        }

        PlayerList self = (PlayerList) (Object) this;

        for (ServerPlayer player : players) {
            PlayerSaveQueue.enqueue(self, player);
        }

        // Suppress the vanilla all-at-once save; our queue will handle each player.
        ci.cancel();
    }
}
