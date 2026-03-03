package com.lythroo.wavelength.mixin;

import com.lythroo.wavelength.client.gui.TopBarHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void wavelength$onMouseButton(long window, MouseInput mouseInput, int action,
                                          CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (mouseInput.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen != null) return;
        if (client.getWindow() == null) return;

        double scaleFactor = client.getWindow().getScaleFactor();
        if (scaleFactor <= 0) return;

        int guiX = (int)(client.mouse.getX() / scaleFactor);
        int guiY = (int)(client.mouse.getY() / scaleFactor);

        TopBarHud.onMouseClick(guiX, guiY);
    }
}