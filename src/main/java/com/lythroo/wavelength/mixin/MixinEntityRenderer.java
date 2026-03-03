package com.lythroo.wavelength.mixin;

import com.lythroo.wavelength.common.data.FriendList;
import com.lythroo.wavelength.common.data.PrivacyMode;
import com.lythroo.wavelength.common.data.RemotePlayerCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(
            method = "renderLabelIfPresent(" +
                    "Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;" +
                    "Lnet/minecraft/client/util/math/MatrixStack;" +
                    "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;" +
                    "Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wavelength$suppressVanillaLabel(PlayerEntityRenderState state,
                                                 MatrixStack matrices,
                                                 OrderedRenderCommandQueue renderQueue,
                                                 CameraRenderState cameraState,
                                                 CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        UUID uuid = null;
        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            if (p.getId() == state.id) {
                uuid = p.getUuid();
                break;
            }
        }
        if (uuid == null) return;

        if (client.player != null && client.player.getUuid().equals(uuid)) {
            ci.cancel();
            return;
        }

        ci.cancel();
    }
}