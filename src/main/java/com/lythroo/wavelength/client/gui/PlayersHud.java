package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.RemotePlayerCache;
import com.lythroo.wavelength.config.WavelengthConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayersHud {

    private static final int BAR_H      = 13;
    private static final int MARGIN_TOP = 4;
    private static final int PAD_H      = 6;
    private static final int SEP_GAP    = 1;

    private static final int COL_SEP         = 0x44FFFFFF;
    private static final int COL_COUNT       = 0xFFAABBDD;
    private static final int COL_NO_NEARBY   = 0xFF8899AA;
    private static final int COL_PLAYER_NAME = 0xFFDDDDFF;

    public static int lastX, lastY, lastW, lastH;

    private record NearbyEntry(String dot, String name, String dist, int dotColor) {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (HudManager.isSuppressed()) return;
        if (client.player == null || client.world == null) return;
        if (client.options.playerListKey.isPressed()) return;
        if (PlayerData.get().pvpMode) { lastW = 0; lastH = 0; return; }

        WavelengthConfig cfg = WavelengthConfig.get();
        if (!cfg.showPrivacySection && !cfg.showNearbySection) { lastW = 0; lastH = 0; return; }

        TextRenderer tr = client.textRenderer;
        AbstractClientPlayerEntity me = client.player;
        PlayerData myData = PlayerData.get();
        PrivacyMode priv = myData.privacy;

        List<AbstractClientPlayerEntity> nearby = new ArrayList<>();
        double range = WavelengthConfig.NEARBY_RANGE_BLOCKS;
        int onlineFriendCount = 0;
        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            if (p == me) continue;
            if (FriendList.isFriend(p.getUuid())) onlineFriendCount++;
            if (p.squaredDistanceTo(me) > range * range) continue;
            PrivacyMode their = RemotePlayerCache.privacy(p.getUuid());
            if (their == PrivacyMode.GHOST) continue;
            if (their == PrivacyMode.FRIENDS && !FriendList.isFriend(p.getUuid())) continue;
            nearby.add(p);
        }
        nearby.sort(Comparator.comparingDouble(p -> p.squaredDistanceTo(me)));
        int max = cfg.maxNearbyShown;
        if (nearby.size() > max) nearby = nearby.subList(0, max);

        List<NearbyEntry> entries = new ArrayList<>();
        for (AbstractClientPlayerEntity p : nearby) {
            ActivityType act = ActivityDetector.getActivity(p.getUuid());
            int dist = (int) Math.sqrt(p.squaredDistanceTo(me));
            String arrow = TopBarHud.bearingArrowToCoords(me, p.getX(), p.getZ());
            entries.add(new NearbyEntry("  \u25CF ", p.getName().getString(),
                    " " + dist + "m " + arrow, act.color));
        }

        String privStr = privacyIcon(priv) + " " + priv.displayName;
        int sec1W = cfg.showPrivacySection ? PAD_H + tr.getWidth(privStr) + PAD_H : 0;

        String countStr = "\uD83D\uDC65 " + nearby.size();
        String friendBadge = onlineFriendCount > 0 ? "  \u2605" + onlineFriendCount : "";
        int sec2W = 0;
        if (cfg.showNearbySection) {
            sec2W = PAD_H + tr.getWidth(countStr) + tr.getWidth(friendBadge);
            if (entries.isEmpty()) {
                sec2W += tr.getWidth("  No players nearby");
            } else {
                for (NearbyEntry e : entries)
                    sec2W += tr.getWidth(e.dot()) + tr.getWidth(e.name()) + tr.getWidth(e.dist());
            }
            sec2W += PAD_H;
        }

        int secs = (sec1W > 0 ? 1 : 0) + (sec2W > 0 ? 1 : 0);
        int totalW = sec1W + sec2W + SEP_GAP * Math.max(0, secs - 1);
        if (totalW == 0) { lastW = 0; lastH = 0; return; }

        int screenW = ctx.getScaledWindowWidth();
        totalW = Math.min(totalW, screenW - 4);

        int barX = (screenW - totalW) / 2 + cfg.playersHudOffsetX;
        int barY = MARGIN_TOP + cfg.playersHudOffsetY;
        barX = Math.max(0, Math.min(barX, screenW - totalW));
        barY = Math.max(0, barY);

        lastX = barX; lastY = barY; lastW = totalW; lastH = BAR_H;

        int bgAlpha = (int)(cfg.topBarOpacity / 100.0 * 255) & 0xFF;
        int bg = (bgAlpha << 24) | 0x08080F;
        drawRoundedRect(ctx, barX, barY, totalW, BAR_H, bg);

        int textY = (BAR_H - tr.fontHeight) / 2;
        int cx = barX;
        boolean first = true;

        if (cfg.showPrivacySection) {
            ctx.drawTextWithShadow(tr, privStr, cx + PAD_H, barY + textY, priv.dotColor);
            cx += sec1W;
            first = false;
        }

        if (cfg.showNearbySection) {
            if (!first) { ctx.fill(cx, barY + 2, cx + SEP_GAP, barY + BAR_H - 2, COL_SEP); cx += SEP_GAP; }
            ctx.drawTextWithShadow(tr, countStr, cx + PAD_H, barY + textY, COL_COUNT);
            cx += PAD_H + tr.getWidth(countStr);
            if (!friendBadge.isEmpty()) {
                ctx.drawTextWithShadow(tr, friendBadge, cx, barY + textY, 0xFFFFD700);
                cx += tr.getWidth(friendBadge);
            }
            if (entries.isEmpty()) {
                ctx.drawTextWithShadow(tr, "  No players nearby", cx, barY + textY, COL_NO_NEARBY);
            } else {
                for (NearbyEntry e : entries) {
                    ctx.drawTextWithShadow(tr, e.dot(), cx, barY + textY, e.dotColor());
                    cx += tr.getWidth(e.dot());
                    ctx.drawTextWithShadow(tr, e.name(), cx, barY + textY, COL_PLAYER_NAME);
                    cx += tr.getWidth(e.name());
                    ctx.drawTextWithShadow(tr, e.dist(), cx, barY + textY, COL_NO_NEARBY);
                    cx += tr.getWidth(e.dist());
                }
            }
        }
    }

    static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x + 2, y,     x + w - 2, y + 2,     col);
        ctx.fill(x,     y + 2, x + w,     y + h - 2, col);
        ctx.fill(x + 2, y + h - 2, x + w - 2, y + h, col);
        ctx.fill(x + 1, y + 1, x + 2,     y + 2,     col);
        ctx.fill(x + w - 2, y + 1, x + w - 1, y + 2, col);
        ctx.fill(x + 1, y + h - 2, x + 2, y + h - 1, col);
        ctx.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, col);
    }

    private static String privacyIcon(PrivacyMode p) {
        return switch (p) { case PUBLIC -> "\u25CF"; case FRIENDS -> "\u2605"; case GHOST -> "\u25CB"; };
    }
}