package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.config.WavelengthConfig;
import com.lythroo.wavelength.WavelengthClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class TopBarHud {

    private static final int BAR_H      = 14;
    private static final int MARGIN_TOP = 4 + 13 + 2;
    private static final int PAD_H      = 6;

    private static final int PVP_PAD_X  = 6;
    private static final int PVP_PAD_Y  = 2;
    private static final int PVP_MARGIN = 6;
    private static int pvpBtnX = -1, pvpBtnY = -1, pvpBtnW = 0, pvpBtnH = 0;

    public static int lastX, lastY, lastW, lastH;

    public static void onMouseClick(int guiX, int guiY) {
        if (pvpBtnW <= 0) return;
        if (guiX >= pvpBtnX && guiX <= pvpBtnX + pvpBtnW
                && guiY >= pvpBtnY && guiY <= pvpBtnY + pvpBtnH) {
            PlayerData.get().togglePvpMode();
            WavelengthClient.pushLocalPlayerData();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                if (PlayerData.get().pvpMode)
                    mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
                else
                    mc.player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.9f);
            }
        }
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (HudManager.isSuppressed()) return;
        if (client.player == null) return;
        if (client.options.playerListKey.isPressed()) return;

        renderPvpPill(ctx, client);
        if (PlayerData.get().pvpMode) { lastW = 0; lastH = 0; return; }

        PlayerData myData = PlayerData.get();
        String rankLine = myData.rankLine();
        String identityDisplay = !rankLine.isBlank() ? rankLine
                : (myData.townName != null && !myData.townName.isBlank() ? myData.townName : "");
        if (identityDisplay.isBlank()) { lastW = 0; lastH = 0; return; }

        WavelengthConfig cfg = WavelengthConfig.get();
        if (!cfg.showTownSection) { lastW = 0; lastH = 0; return; }
        TextRenderer tr = client.textRenderer;
        PrivacyMode priv = myData.privacy;

        String dot = privacyIcon(priv);
        String idFull = dot + " " + identityDisplay;
        int totalW = Math.min(PAD_H + tr.getWidth(idFull) + PAD_H,
                ctx.getScaledWindowWidth() - 4);

        int screenW = ctx.getScaledWindowWidth();
        int barX = (screenW - totalW) / 2 + cfg.topBarOffsetX;
        int barY = MARGIN_TOP + cfg.topBarOffsetY;
        barX = Math.max(0, Math.min(barX, screenW - totalW));
        barY = Math.max(0, barY);

        lastX = barX; lastY = barY; lastW = totalW; lastH = BAR_H;

        int bgAlpha = (int)(cfg.topBarOpacity / 100.0 * 255) & 0xFF;
        int bg = (bgAlpha << 24) | 0x08080F;
        PlayersHud.drawRoundedRect(ctx, barX, barY, totalW, BAR_H, bg);

        ctx.fill(barX + 2, barY, barX + totalW - 2, barY + 1, 0x22FFFFFF);

        int dotW = tr.getWidth(dot + " ");
        int fullW = tr.getWidth(idFull);
        int textX = barX + (totalW - fullW) / 2;
        int textY = barY + (BAR_H - tr.fontHeight) / 2;
        ctx.drawTextWithShadow(tr, dot, textX, textY, priv.dotColor);
        ctx.drawTextWithShadow(tr, identityDisplay, textX + dotW, textY, 0xFFDDBB66);
    }

    private static void renderPvpPill(DrawContext ctx, MinecraftClient client) {
        if (!PlayerData.get().pvpMode) { pvpBtnX = -1; pvpBtnY = -1; pvpBtnW = 0; pvpBtnH = 0; return; }
        TextRenderer tr = client.textRenderer;
        String text = "\u2694 PVP: ON";
        int pillW = tr.getWidth(text) + PVP_PAD_X * 2;
        int pillH = tr.fontHeight + PVP_PAD_Y * 2;
        int px = ctx.getScaledWindowWidth()  - pillW - PVP_MARGIN;
        int py = ctx.getScaledWindowHeight() - pillH - PVP_MARGIN;
        pvpBtnX = px; pvpBtnY = py; pvpBtnW = pillW; pvpBtnH = pillH;
        ctx.fill(px + 1, py, px + pillW - 1, py + pillH, 0xFFAA3333);
        ctx.fill(px, py + 1, px + pillW, py + pillH - 1, 0xFFAA3333);
        ctx.fill(px + 1, py + 1, px + pillW - 1, py + pillH - 1, 0xCC3A0A0A);
        ctx.drawTextWithShadow(tr, text, px + PVP_PAD_X, py + PVP_PAD_Y, 0xFFFF6666);
    }

    public static String bearingArrowToCoords(AbstractClientPlayerEntity observer,
                                              double targetX, double targetZ) {
        double dx = targetX - observer.getX();
        double dz = targetZ - observer.getZ();
        double worldCompass = Math.toDegrees(Math.atan2(dx, -dz));
        if (worldCompass < 0) worldCompass += 360;
        double observerCompass = (observer.getYaw() + 180.0) % 360.0;
        if (observerCompass < 0) observerCompass += 360;
        double rel = worldCompass - observerCompass;
        rel = ((rel + 540) % 360) - 180;
        if (rel >= -22.5  && rel <  22.5)  return "\u2191";
        if (rel >=  22.5  && rel <  67.5)  return "\u2197";
        if (rel >=  67.5  && rel < 112.5)  return "\u2192";
        if (rel >= 112.5  && rel < 157.5)  return "\u2198";
        if (rel >= 157.5  || rel < -157.5) return "\u2193";
        if (rel >= -157.5 && rel < -112.5) return "\u2199";
        if (rel >= -112.5 && rel <  -67.5) return "\u2190";
        return "\u2196";
    }

    private static String privacyIcon(PrivacyMode p) {
        return switch (p) { case PUBLIC -> "\u25CF"; case FRIENDS -> "\u2605"; case GHOST -> "\u25CB"; };
    }
}