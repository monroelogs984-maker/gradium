package com.glennfo.gradium.mixin;

import com.glennfo.gradium.PlayerSaveQueue;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow
    private List<ServerPlayerEntity> players;

    @Inject(method = "saveAll", at = @At("HEAD"), cancellable = true)
    private void gradium$interceptSaveAll(CallbackInfo ci) {
        if (PlayerSaveQueue.processing) return;

        PlayerList self = (PlayerList) (Object) this;

        for (ServerPlayerEntity player : players) {
            PlayerSaveQueue.enqueue(self, player);
        }

        ci.cancel();
    }
}
