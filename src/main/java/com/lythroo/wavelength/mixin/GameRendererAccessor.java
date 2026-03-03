package com.lythroo.wavelength.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor float getFovMultiplier();
    @Accessor float getLastFovMultiplier();

    @Invoker("bobView")
    void invokedBobView(MatrixStack matrices, float tickDelta);
}