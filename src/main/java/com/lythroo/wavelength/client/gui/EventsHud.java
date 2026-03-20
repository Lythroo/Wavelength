package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.gui.HudManager;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.config.WavelengthConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class EventsHud {

    private static final int MARGIN_RIGHT = 6;
    private static final int MARGIN_TOP   = 4;
    private static final int PAD_X        = 6;
    private static final int PAD_Y        = 3;
    private static final int LINE_GAP     = 1;
    private static final int MAX_NAME_LEN = 18;
    private static final int MAX_WIDTH    = 160;

    private static final int COL_EVENT       = 0xFF55DDFF;
    private static final int COL_EVENT_SCHED = 0xFFFFAA44;
    private static final int COL_EVENT_TIME  = 0xFF7799BB;

    public static int lastX, lastY, lastW, lastH;

    private record EventEntry(String icon, String name, String chip,
                              int nameColor, long remainMs) {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (HudManager.isSuppressed()) return;
        if (client.options.playerListKey.isPressed()) return;

        WavelengthConfig cfg = WavelengthConfig.get();
        if (PlayerData.get().pvpMode) return;
        if (!cfg.showEventSection) {
            lastW = 0; lastH = 0;
            return;
        }

        AbstractClientPlayerEntity me = client.player;
        TextRenderer tr = client.textRenderer;

        List<EventEntry> entries = new ArrayList<>();
        for (EventData ev : ClientEventCache.getActive()) {
            if (!ev.active) continue;
            boolean sched = ev.isScheduled();
            String name = ev.name.length() > MAX_NAME_LEN
                    ? ev.name.substring(0, MAX_NAME_LEN) + "…" : ev.name;
            String chip = "";
            long remainMs = 0;
            if (sched) {
                remainMs = ev.scheduledStartMs - System.currentTimeMillis();
                if (remainMs > 0) chip = " ⏰" + fmt(remainMs);
            } else if (ev.endMs > 0) {
                remainMs = ev.endMs - System.currentTimeMillis();
                chip = remainMs > 0 ? " ⏱" + fmt(remainMs) : " ✓";
            }
            if (ev.hasLocation && me != null) {
                chip += " " + TopBarHud.bearingArrowToCoords(me, ev.locX, ev.locZ);
            }
            entries.add(new EventEntry("📅 ", name, chip,
                    sched ? COL_EVENT_SCHED : COL_EVENT, remainMs));
        }

        if (entries.isEmpty()) { lastW = 0; lastH = 0; return; }

        int lineH = tr.fontHeight + PAD_Y * 2;
        int contentW = 0;
        for (EventEntry e : entries) {
            int w = tr.getWidth(e.icon() + e.name() + e.chip());
            if (w > contentW) contentW = w;
        }
        int panelW = Math.min(MAX_WIDTH, contentW + PAD_X * 2);
        int panelH = entries.size() * lineH + Math.max(0, entries.size() - 1) * LINE_GAP;

        int screenW = ctx.getScaledWindowWidth();
        int px = screenW - panelW - MARGIN_RIGHT + cfg.eventsHudOffsetX;
        int py = MARGIN_TOP + cfg.eventsHudOffsetY;

        px = Math.max(0, Math.min(px, screenW - panelW));
        py = Math.max(0, Math.min(py, ctx.getScaledWindowHeight() - panelH));

        lastX = px; lastY = py; lastW = panelW; lastH = panelH;

        renderPanel(ctx, tr, entries, px, py, panelW, panelH, lineH, cfg);
    }

    static void renderPanel(DrawContext ctx, TextRenderer tr, List<EventEntry> entries,
                            int px, int py, int panelW, int panelH, int lineH,
                            WavelengthConfig cfg) {
        int bgAlpha = (int)(cfg.topBarOpacity / 100.0 * 255) & 0xFF;
        int bg = (bgAlpha << 24) | 0x08080F;

        ctx.fill(px + 2, py,          px + panelW - 2, py + 2,           bg);
        ctx.fill(px,     py + 2,      px + panelW,     py + panelH - 2,  bg);
        ctx.fill(px + 2, py + panelH - 2, px + panelW - 2, py + panelH, bg);
        ctx.fill(px + 1, py + 1,      px + 2,          py + 2,           bg);
        ctx.fill(px + panelW - 2, py + 1, px + panelW - 1, py + 2,      bg);
        ctx.fill(px + 1, py + panelH - 2, px + 2,      py + panelH - 1, bg);
        ctx.fill(px + panelW - 2, py + panelH - 2, px + panelW - 1, py + panelH - 1, bg);

        int cy = py;
        for (EventEntry e : entries) {
            int ty = cy + PAD_Y;
            int cx = px + PAD_X;
            ctx.drawTextWithShadow(tr, e.icon(), cx, ty, e.nameColor());
            cx += tr.getWidth(e.icon());
            ctx.drawTextWithShadow(tr, e.name(), cx, ty, 0xFFDDEEFF);
            cx += tr.getWidth(e.name());
            if (!e.chip().isEmpty()) {
                ctx.drawTextWithShadow(tr, e.chip(), cx, ty, chipColor(e.remainMs()));
            }
            cy += lineH + LINE_GAP;
        }
    }

    public static String fmt(long ms) {
        if (ms <= 0) return "now";
        long s = ms / 1_000;
        if (s < 60)   return s + "s";
        long m = s / 60, rs = s % 60;
        if (m < 5)    return m + "m" + (rs > 0 ? rs + "s" : "");
        if (m < 60)   return m + "m";
        long h = m / 60, rm = m % 60;
        if (h < 24)   return h + "h" + (rm > 0 ? rm + "m" : "");
        return (h / 24) + "d" + (h % 24 > 0 ? (h % 24) + "h" : "");
    }

    private static int chipColor(long ms) {
        long s = ms / 1_000;
        if (s < 60)  return 0xFFFF5555;
        if (s < 300) return 0xFFFFAA44;
        return COL_EVENT_TIME;
    }
}