package com.glennfo.gradium.mixin;

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

    @Inject(method = "saveAll", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSaveAll(CallbackInfo ci) {
        if (PlayerSaveQueue.processing) return;

        PlayerList self = (PlayerList) (Object) this;

        for (ServerPlayer player : players) {
            PlayerSaveQueue.enqueue(self, player);
        }

        ci.cancel();
    }
}
