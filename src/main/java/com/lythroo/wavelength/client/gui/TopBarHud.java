package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.client.gui.HudManager;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.ClientEventCache;
import com.lythroo.wavelength.common.data.EventData;
import com.lythroo.wavelength.common.data.RemotePlayerCache;
import com.lythroo.wavelength.config.WavelengthConfig;
import com.lythroo.wavelength.WavelengthClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TopBarHud {

    private static final int BAR_H      = 13;
    private static final int BAR_ID     = 14;
    private static final int MARGIN_TOP = 4;
    private static final int PAD_H      = 6;
    private static final int PAD_ID     = 6;
    private static final int SEP_GAP    = 1;

    private static final int COL_SEP    = 0x44FFFFFF;
    private static final int COL_DIV    = 0x22FFFFFF;
    private static final int COL_ID_DIV = 0x22FFFFFF;

    private static final int PVP_PAD_X  = 6;
    private static final int PVP_PAD_Y  = 2;
    private static final int PVP_MARGIN = 6;
    private static int pvpBtnX = -1, pvpBtnY = -1, pvpBtnW = 0, pvpBtnH = 0;

    public static void onMouseClick(int guiX, int guiY) {
        if (pvpBtnW <= 0) return;
        if (guiX >= pvpBtnX && guiX <= pvpBtnX + pvpBtnW
                && guiY >= pvpBtnY && guiY <= pvpBtnY + pvpBtnH) {
            PlayerData.get().togglePvpMode();
            WavelengthClient.pushLocalPlayerData();
        }
    }

    private static final int COL_COUNT       = 0xFFAABBDD;
    private static final int COL_NO_NEARBY   = 0xFF8899AA;
    private static final int COL_PLAYER_NAME = 0xFFDDDDFF;
    private static final int COL_PLACEHOLDER  = 0xFF8899BB;
    private static final int COL_PH_DIM       = 0xFF556677;
    private static final int COL_TOWN         = 0xFFDDBB66;
    private static final int COL_EVENT        = 0xFF55DDFF;
    private static final int COL_EVENT_SCHED  = 0xFFFFAA44;
    private static final int COL_EVENT_TIME   = 0xFF7799BB;

    private record NearbyEntry(String dot, String name, String dist,
                               String arrow, int dotColor) {}

    private record EventHudEntry(String icon, String name, String timeChip,
                                 String arrow, int nameColor, boolean isScheduled,
                                 long remainingMs) {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (HudManager.isSuppressed()) return;
        if (client.player == null || client.world == null) return;
        if (client.options.playerListKey.isPressed()) return;

        renderPvpPill(ctx, client);
        if (PlayerData.get().pvpMode) return;

        WavelengthConfig cfg = WavelengthConfig.get();
        TextRenderer tr = client.textRenderer;

        AbstractClientPlayerEntity me = client.player;
        PlayerData myData = PlayerData.get();
        PrivacyMode priv = myData.privacy;

        String rankLine = myData.rankLine();

        String identityDisplay = !rankLine.isBlank() ? rankLine
                : (myData.townName != null && !myData.townName.isBlank() ? myData.townName : "");
        boolean hasIdentity = !identityDisplay.isBlank();

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

        List<NearbyEntry> nearbyEntries = new ArrayList<>();
        for (AbstractClientPlayerEntity p : nearby) {
            ActivityType act = ActivityDetector.getActivity(p.getUuid());
            int dist = (int) Math.sqrt(p.squaredDistanceTo(me));
            String arrow = bearingArrowToPlayer(me, p);
            nearbyEntries.add(new NearbyEntry("  \u25CF ", p.getName().getString(),
                    " " + dist + "m " + arrow, arrow, act.color));
        }

        String privStr = privacyIcon(priv) + " " + priv.displayName;
        int sec1W = cfg.showPrivacySection ? PAD_H + tr.getWidth(privStr) + PAD_H : 0;

        String countStr = "\uD83D\uDC65 " + nearby.size();

        String friendBadge = onlineFriendCount > 0 ? "  \u2605" + onlineFriendCount : "";
        int sec2W = 0;
        if (cfg.showNearbySection) {
            sec2W = PAD_H + tr.getWidth(countStr) + tr.getWidth(friendBadge);
            if (nearbyEntries.isEmpty()) {
                sec2W += tr.getWidth("  No players nearby");
            } else {
                for (NearbyEntry e : nearbyEntries)
                    sec2W += tr.getWidth(e.dot()) + tr.getWidth(e.name()) + tr.getWidth(e.dist());
            }
            sec2W += PAD_H;
        }

        int row1SecCount = (sec1W > 0 ? 1 : 0) + (sec2W > 0 ? 1 : 0);
        int row1W = sec1W + sec2W + SEP_GAP * Math.max(0, row1SecCount - 1);

        String townName = myData.townName != null ? myData.townName.trim() : "";
        String townStr = townName.isEmpty() ? "" : "\uD83D\uDCCD " + townName;
        int sec3W = (cfg.showTownSection && !townStr.isEmpty())
                ? PAD_H + tr.getWidth(townStr) + PAD_H : 0;

        final int MAX_EVENT_NAME = 14;
        List<EventHudEntry> eventHudEntries = new ArrayList<>();
        if (cfg.showEventSection) {
            for (EventData ev : ClientEventCache.getActive()) {
                if (!ev.active) continue;
                boolean sched = ev.isScheduled();
                String nameStr = ev.name.length() > MAX_EVENT_NAME
                        ? ev.name.substring(0, MAX_EVENT_NAME) + "\u2026" : ev.name;

                String timeChip  = "";
                long   remainMs  = 0;
                if (sched) {
                    remainMs = ev.scheduledStartMs - System.currentTimeMillis();
                    if (remainMs > 0) timeChip = " \u23F0" + fmtCountdown(remainMs);
                } else if (ev.endMs > 0) {
                    remainMs = ev.endMs - System.currentTimeMillis();
                    if (remainMs > 0) timeChip = " \u23F1" + fmtCountdown(remainMs);
                    else             timeChip = " \u2713";
                }

                String arrow = ev.hasLocation ? bearingArrowToCoords(me, ev.locX, ev.locZ) : "";
                if (!arrow.isEmpty()) timeChip += " " + arrow;

                eventHudEntries.add(new EventHudEntry(
                        "\uD83D\uDCC5 ", nameStr, timeChip,
                        arrow, sched ? COL_EVENT_SCHED : COL_EVENT, sched, remainMs));
            }
        }

        int screenW = ctx.getScaledWindowWidth();
        int maxBarW = screenW - 4;
        int sec4W = 0;
        List<EventHudEntry> visibleEvents = new ArrayList<>();
        if (cfg.showEventSection) {
            int tentative = PAD_H;
            if (eventHudEntries.isEmpty()) {
                tentative += tr.getWidth("\uD83D\uDCC5 No events") + PAD_H;
                sec4W = tentative;
            } else {
                for (EventHudEntry ee : eventHudEntries) {
                    int entryW = (visibleEvents.isEmpty() ? 0 : tr.getWidth("  \u00B7  "))
                            + tr.getWidth(ee.icon()) + tr.getWidth(ee.name())
                            + tr.getWidth(ee.timeChip());

                    int projectedTotal = Math.max(row1W, sec3W + tentative + entryW + PAD_H)
                            + SEP_GAP * (sec3W > 0 ? 1 : 0);
                    if (projectedTotal > maxBarW && !visibleEvents.isEmpty()) break;
                    tentative += entryW;
                    visibleEvents.add(ee);
                }
                sec4W = tentative + PAD_H;
            }
        }

        int row2SecCount = (sec3W > 0 ? 1 : 0) + (sec4W > 0 ? 1 : 0);
        int row2W = sec3W + sec4W + SEP_GAP * Math.max(0, row2SecCount - 1);
        boolean hasRow2 = row2W > 0;

        if (row1W == 0 && row2W == 0) return;

        String idFull = hasIdentity ? privacyIcon(priv) + " " + identityDisplay : "";
        int minWForId = hasIdentity ? PAD_ID + tr.getWidth(idFull) + PAD_ID : 0;

        screenW = ctx.getScaledWindowWidth();
        int totalW = Math.min(
                Math.max(Math.max(row1W, row2W), minWForId),
                screenW - 4);

        screenW = ctx.getScaledWindowWidth();
        int barX = (screenW - totalW) / 2;
        int barY = MARGIN_TOP;

        int totalH = (row1W > 0 ? BAR_H : 0)
                + (hasRow2 ? 1 + BAR_H : 0)
                + (hasIdentity ? BAR_ID : 0);

        if (totalH == 0) return;

        int bgAlpha = (int) (cfg.topBarOpacity / 100.0 * 255) & 0xFF;
        int COL_BG = (bgAlpha << 24) | 0x08080F;

        ctx.fill(barX + 2, barY, barX + totalW - 2, barY + 2, COL_BG);
        ctx.fill(barX, barY + 2, barX + totalW, barY + totalH - 2, COL_BG);
        ctx.fill(barX + 2, barY + totalH - 2, barX + totalW - 2, barY + totalH, COL_BG);
        ctx.fill(barX + 1, barY + 1, barX + 2, barY + 2, COL_BG);
        ctx.fill(barX + totalW - 2, barY + 1, barX + totalW - 1, barY + 2, COL_BG);
        ctx.fill(barX + 1, barY + totalH - 2, barX + 2, barY + totalH - 1, COL_BG);
        ctx.fill(barX + totalW - 2, barY + totalH - 2, barX + totalW - 1, barY + totalH - 1, COL_BG);

        int textY = (BAR_H - tr.fontHeight) / 2;

        if (row1W > 0) {
            int cx = barX;
            boolean first = true;

            if (cfg.showPrivacySection) {
                if (!first) {
                    drawSep(ctx, cx, barY);
                    cx += SEP_GAP;
                }
                ctx.drawTextWithShadow(tr, privStr, cx + PAD_H, barY + textY, priv.dotColor);
                cx += sec1W;
                first = false;
            }

            if (cfg.showNearbySection) {
                if (!first) {
                    drawSep(ctx, cx, barY);
                    cx += SEP_GAP;
                }
                ctx.drawTextWithShadow(tr, countStr, cx + PAD_H, barY + textY, COL_COUNT);
                cx += PAD_H + tr.getWidth(countStr);

                if (!friendBadge.isEmpty()) {
                    ctx.drawTextWithShadow(tr, friendBadge, cx, barY + textY, 0xFFFFD700);
                    cx += tr.getWidth(friendBadge);
                }
                if (nearbyEntries.isEmpty()) {
                    ctx.drawTextWithShadow(tr, "  No players nearby", cx, barY + textY, COL_NO_NEARBY);
                } else {
                    for (NearbyEntry e : nearbyEntries) {
                        ctx.drawTextWithShadow(tr, e.dot(), cx, barY + textY, e.dotColor());
                        cx += tr.getWidth(e.dot());
                        ctx.drawTextWithShadow(tr, e.name(), cx, barY + textY, COL_PLAYER_NAME);
                        cx += tr.getWidth(e.name());
                        ctx.drawTextWithShadow(tr, e.dist(), cx, barY + textY, COL_NO_NEARBY);
                        cx += tr.getWidth(e.dist());
                    }
                }
                first = false;
            }
        }

        if (hasRow2) {
            int row2Y = barY + BAR_H;
            ctx.fill(barX + 2, row2Y, barX + totalW - 2, row2Y + 1, COL_DIV);

            int cx2 = barX;
            boolean f2 = true;

            if (cfg.showTownSection && sec3W > 0) {
                if (!f2) {
                    drawSep(ctx, cx2, row2Y + 1);
                    cx2 += SEP_GAP;
                }
                ctx.drawTextWithShadow(tr, townStr, cx2 + PAD_H, row2Y + 1 + textY, COL_TOWN);
                cx2 += sec3W;
                f2 = false;
            }

            if (cfg.showEventSection && sec4W > 0) {
                if (!f2) {
                    drawSep(ctx, cx2, row2Y + 1);
                    cx2 += SEP_GAP;
                }

                if (visibleEvents.isEmpty()) {
                    ctx.drawTextWithShadow(tr, "\uD83D\uDCC5 No events",
                            cx2 + PAD_H, row2Y + 1 + textY, COL_PH_DIM);
                    cx2 += PAD_H + tr.getWidth("\uD83D\uDCC5 No events");
                } else {
                    cx2 += PAD_H;
                    for (int i = 0; i < visibleEvents.size(); i++) {
                        EventHudEntry ee = visibleEvents.get(i);
                        if (i > 0) {
                            ctx.drawTextWithShadow(tr, "  \u00B7  ", cx2, row2Y + 1 + textY, COL_PH_DIM);
                            cx2 += tr.getWidth("  \u00B7  ");
                        }
                        ctx.drawTextWithShadow(tr, ee.icon(), cx2, row2Y + 1 + textY, ee.nameColor());
                        cx2 += tr.getWidth(ee.icon());
                        ctx.drawTextWithShadow(tr, ee.name(), cx2, row2Y + 1 + textY, 0xFFDDEEFF);
                        cx2 += tr.getWidth(ee.name());
                        if (!ee.timeChip().isEmpty()) {
                            int chipColor = ee.remainingMs() > 0
                                    ? timeChipColor(ee.remainingMs()) : COL_EVENT_TIME;
                            ctx.drawTextWithShadow(tr, ee.timeChip(), cx2, row2Y + 1 + textY, chipColor);
                            cx2 += tr.getWidth(ee.timeChip());
                        }
                    }
                    cx2 += PAD_H;
                }
                f2 = false;
            }
        }

        if (hasIdentity) {
            int idY = barY + BAR_H + (hasRow2 ? 1 + BAR_H : 0);
            ctx.fill(barX + 2, idY, barX + totalW - 2, idY + 1, COL_ID_DIV);

            int idTextY = idY + 1 + (BAR_ID - 1 - tr.fontHeight) / 2;
            String dot = privacyIcon(priv);
            int dotW = tr.getWidth(dot + " ");
            int idFullW = tr.getWidth(idFull);
            int idTextX = barX + (totalW - idFullW) / 2;
            ctx.drawTextWithShadow(tr, dot, idTextX, idTextY, priv.dotColor);
            ctx.drawTextWithShadow(tr, identityDisplay, idTextX + dotW, idTextY, 0xFFDDBB66);
        }
    }

    private static void renderPvpPill(DrawContext ctx, MinecraftClient client) {
        if (!PlayerData.get().pvpMode) {
            pvpBtnX = -1; pvpBtnY = -1; pvpBtnW = 0; pvpBtnH = 0;
            return;
        }
        TextRenderer tr    = client.textRenderer;
        String       text  = "\u2694 PVP: ON";
        int pillW = tr.getWidth(text) + PVP_PAD_X * 2;
        int pillH = tr.fontHeight     + PVP_PAD_Y * 2;
        int px    = ctx.getScaledWindowWidth()  - pillW - PVP_MARGIN;
        int py    = ctx.getScaledWindowHeight() - pillH - PVP_MARGIN;

        pvpBtnX = px; pvpBtnY = py; pvpBtnW = pillW; pvpBtnH = pillH;

        ctx.fill(px + 1, py,     px + pillW - 1, py + pillH,     0xFFAA3333);
        ctx.fill(px,     py + 1, px + pillW,     py + pillH - 1, 0xFFAA3333);
        ctx.fill(px + 1, py + 1, px + pillW - 1, py + pillH - 1, 0xCC3A0A0A);
        ctx.drawTextWithShadow(tr, text, px + PVP_PAD_X, py + PVP_PAD_Y, 0xFFFF6666);
    }

    private static String fmtCountdown(long ms) {
        if (ms <= 0) return "now";
        long totalSecs = ms / 1_000;
        if (totalSecs < 60)  return totalSecs + "s";
        long mins    = totalSecs / 60;
        long remSecs = totalSecs % 60;
        if (mins < 5)   return mins + "m" + (remSecs > 0 ? remSecs + "s" : "");
        if (mins < 60)  return mins + "m";
        long hours   = mins / 60;
        long remMins = mins % 60;
        if (hours < 24) return hours + "h" + (remMins > 0 ? remMins + "m" : "");
        return (hours / 24) + "d" + (hours % 24 > 0 ? (hours % 24) + "h" : "");
    }

    private static int timeChipColor(long remainingMs) {
        long totalSecs = remainingMs / 1_000;
        if (totalSecs < 60)   return 0xFFFF5555;
        if (totalSecs < 300)  return 0xFFFFAA44;
        return COL_EVENT_TIME;
    }

    private static void drawSep(DrawContext ctx, int x, int y) {
        ctx.fill(x, y + 2, x + SEP_GAP, y + BAR_H - 2, COL_SEP);
    }

    private static String privacyIcon(PrivacyMode p) {
        return switch (p) {
            case PUBLIC  -> "\u25CF";
            case FRIENDS -> "\u2605";
            case GHOST   -> "\u25CB";
        };
    }

    private static String bearingArrowToPlayer(AbstractClientPlayerEntity observer,
                                               AbstractClientPlayerEntity target) {
        return bearingArrowToCoords(observer, target.getX(), target.getZ());
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
}