package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.render.AnimationHelper;
import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.client.tracking.StatTracker;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.RemotePlayerCache;
import com.lythroo.wavelength.common.data.RemoteStatCache;
import com.lythroo.wavelength.common.network.ServerModDetector;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.*;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCardComponent extends BaseUIComponent {

    public static final int CARD_W     = 165;
    public static final int CARD_H     = 290;
    private static final int FACE_SIZE = 64;
    private static final int PAD       = 8;
    private static final int BAR_H     = 4;
    private static final int INNER_W   = CARD_W - 2 * PAD;

    private static final int DIV1_Y  = PAD + FACE_SIZE + 4;
    private static final int PLAY_Y  = DIV1_Y + 7;
    private static final int DIV2_Y  = PLAY_Y + 24;
    private static final int BADGE_Y = DIV2_Y + 6;
    private static final int DIV3_Y  = BADGE_Y + 46;
    private static final int STATS_Y = DIV3_Y + 6;
    private static final int DIV4_Y  = STATS_Y + 59;
    private static final int ACTS_Y  = DIV4_Y + 6;

    private static final int COL_CARD_BG    = 0xCC1A1A2E;
    private static final int COL_CARD_BG_HV = 0xCC252545;
    private static final int COL_BORDER     = 0xFF2E2E5E;
    private static final int COL_BORDER_HV  = 0xFFAA88FF;
    private static final int COL_DIVIDER    = 0x44FFFFFF;
    private static final int COL_WHITE      = 0xFFFFFFFF;
    private static final int COL_GRAY       = 0xFFAAAAAA;
    private static final int COL_GOLD       = 0xFFFFD700;
    private static final int COL_BAR_BG     = 0xFF333333;
    private static final int COL_SECTION    = 0xFF9988BB;

    private static class RecentSnapshot {
        long blocksMined, blocksPlaced, mobsKilled, cropsHarvested, itemsCrafted;
        List<String> actions = new ArrayList<>();
        long lastUpdateMs    = 0;
        static final long UPDATE_MS = 15_000;
    }

    private static final Map<UUID, RecentSnapshot> recentSnapshots = new ConcurrentHashMap<>();

    private static List<String> getRecentActions(UUID uuid,
                                                 long mined, long placed,
                                                 long killed, long crops, long crafted) {
        long now = System.currentTimeMillis();
        RecentSnapshot snap = recentSnapshots.computeIfAbsent(uuid, k -> {
            RecentSnapshot s  = new RecentSnapshot();
            s.blocksMined     = mined;
            s.blocksPlaced    = placed;
            s.mobsKilled      = killed;
            s.cropsHarvested  = crops;
            s.itemsCrafted    = crafted;
            s.lastUpdateMs    = now;
            return s;
        });

        if (now - snap.lastUpdateMs < RecentSnapshot.UPDATE_MS) return snap.actions;

        long dMined   = Math.max(0, mined   - snap.blocksMined);
        long dPlaced  = Math.max(0, placed  - snap.blocksPlaced);
        long dKilled  = Math.max(0, killed  - snap.mobsKilled);
        long dCrops   = Math.max(0, crops   - snap.cropsHarvested);
        long dCrafted = Math.max(0, crafted - snap.itemsCrafted);

        List<String> next = new ArrayList<>();
        if (dMined   > 0) next.add("Mined "     + formatNum(dMined)   + " blocks");
        if (dPlaced  > 0) next.add("Placed "    + formatNum(dPlaced)  + " blocks");
        if (dKilled  > 0) next.add("Killed "    + formatNum(dKilled)  + (dKilled == 1 ? " entity" : " entities"));
        if (dCrops   > 0) next.add("Harvested " + formatNum(dCrops)   + " crops");
        if (dCrafted > 0) next.add("Crafted "   + formatNum(dCrafted) + " items");
        if (!next.isEmpty()) snap.actions = next.size() > 3 ? next.subList(0, 3) : next;

        snap.blocksMined    = mined;
        snap.blocksPlaced   = placed;
        snap.mobsKilled     = killed;
        snap.cropsHarvested = crops;
        snap.itemsCrafted   = crafted;
        snap.lastUpdateMs   = now;
        return snap.actions;
    }

    public static void clearRecentActions(UUID uuid) { recentSnapshots.remove(uuid); }

    private final AbstractClientPlayerEntity player;
    private final UUID   uuid;
    private final String username;
    private float hoverAnim = 0f;

    public PlayerCardComponent(AbstractClientPlayerEntity player) {
        this.player   = player;
        this.uuid     = player.getUuid();
        this.username = player.getName().getString();
        this.sizing(Sizing.fixed(CARD_W), Sizing.fixed(CARD_H));
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        float target = isInBounds(mouseX, mouseY) ? 1f : 0f;
        hoverAnim += (target - hoverAnim) * Math.min(delta * 0.25f, 1f);
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY,
                     float partialTicks, float delta) {
        final int x = this.x;
        final int y = this.y;

        MinecraftClient client  = MinecraftClient.getInstance();
        TextRenderer    tr      = client.textRenderer;
        ActivityType    act     = ActivityDetector.getActivity(uuid);
        PlayerData      myData  = PlayerData.get();
        StatData        myStats = StatTracker.getData();

        boolean isLocal = client.player != null
                && (uuid.equals(client.player.getUuid())
                || username.equals(client.player.getName().getString()));

        RemotePlayerCache.Entry remoteEntry = isLocal ? null : RemotePlayerCache.get(uuid);
        RemoteStatCache.Entry   remoteStats = isLocal ? null : RemoteStatCache.get(uuid);

        boolean ghostMode = isLocal
                ? myData.privacy == PrivacyMode.GHOST
                : remoteEntry != null && remoteEntry.privacy() == PrivacyMode.GHOST;

        graphics.fill(x, y, x + CARD_W, y + CARD_H,
                lerpColor(COL_CARD_BG, COL_CARD_BG_HV, hoverAnim));
        int borderCol = lerpColor(COL_BORDER, COL_BORDER_HV, hoverAnim);
        drawBorder(graphics, x, y, CARD_W, CARD_H, borderCol);
        if (hoverAnim > 0.01f) {
            drawBorder(graphics, x-1, y-1, CARD_W+2, CARD_H+2,
                    ((int)(hoverAnim*40)<<24)|(COL_BORDER_HV&0x00FFFFFF));
            if (hoverAnim > 0.5f)
                drawBorder(graphics, x-2, y-2, CARD_W+4, CARD_H+4,
                        ((int)(hoverAnim*18)<<24)|(COL_BORDER_HV&0x00FFFFFF));
        }
        if (ghostMode) graphics.fill(x, y, x+CARD_W, y+CARD_H, 0x88000000);

        int faceX = x + PAD, faceY = y + PAD;
        graphics.fill(faceX, faceY, faceX + FACE_SIZE, faceY + FACE_SIZE, 0xFF111122);
        drawBorder(graphics, faceX, faceY, FACE_SIZE, FACE_SIZE,
                lerpColor(0x44FFFFFF, 0xAABBAAFF, hoverAnim));
        Identifier skin = player.getSkin().body().texturePath();
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        graphics.drawTexture(pipeline, skin, faceX, faceY,  8f, 8f, FACE_SIZE, FACE_SIZE, 8, 8, 64, 64);
        graphics.drawTexture(pipeline, skin, faceX, faceY, 40f, 8f, FACE_SIZE, FACE_SIZE, 8, 8, 64, 64);
        if (!ghostMode) {
            String emoji = activityEmoji(act);
            graphics.drawText(tr, emoji,
                    faceX + FACE_SIZE - tr.getWidth(emoji) - 2,
                    faceY + FACE_SIZE - 10, act.color, true);
        }

        int infoX = faceX + FACE_SIZE + 6;
        int infoY = y + PAD;
        int maxW  = CARD_W - FACE_SIZE - PAD * 3 - 6;

        drawText(graphics, tr, truncate(tr, username, maxW), infoX, infoY,
                isLocal ? 0xFFDDAAFF : COL_WHITE, true);
        infoY += 11;

        if (ghostMode) {
            drawText(graphics, tr, "Ghost Mode",  infoX, infoY, 0xFFFF5555, true);
            infoY += 11;
            drawText(graphics, tr, "Info hidden", infoX, infoY, COL_GRAY,   false);
        } else {
            String rankLine = isLocal ? myData.rankLine()
                    : (remoteEntry != null ? remoteEntry.rankLine() : "");
            if (!rankLine.isEmpty()) {
                drawText(graphics, tr, truncate(tr, rankLine, maxW), infoX, infoY, COL_GOLD, true);
                infoY += 11;
            }
            drawText(graphics, tr, activityEmoji(act) + " " + act.displayName,
                    infoX, infoY, act.color, true);
            infoY += 11;
            if (!isLocal && client.player != null) {
                double dist = Math.sqrt(player.squaredDistanceTo(client.player));

                String arrow = dirArrow(client.player, player);
                drawText(graphics, tr, (int)dist + "m " + arrow,
                        infoX, infoY, COL_GRAY, false);
                infoY += 11;
                if (FriendList.isFriend(uuid))
                    drawText(graphics, tr, "\u2605 Friend", infoX, infoY, COL_GOLD, true);
            } else if (isLocal) {
                PrivacyMode priv = myData.privacy;
                drawText(graphics, tr, privacyDot(priv) + " " + priv.displayName,
                        infoX, infoY, priv.dotColor, true);
            }
        }

        graphics.fill(x + PAD, y + DIV1_Y, x + CARD_W - PAD, y + DIV1_Y + 1, COL_DIVIDER);

        if (ghostMode) {
            String gt = "Stats hidden (Ghost Mode)";
            drawText(graphics, tr, gt, x + CARD_W/2 - tr.getWidth(gt)/2,
                    y + DIV1_Y + 12, 0xFF884444, false);
            return;
        }

        long statMined, statPlaced, statKilled, statCrops, statCrafted, statDistM, statPlaySec;
        boolean hasStats;

        if (isLocal) {
            statMined   = myStats.blocksMined;
            statPlaced  = myStats.blocksPlaced;
            statKilled  = myStats.mobsKilled;
            statCrops   = myStats.cropsHarvested;
            statCrafted = myStats.itemsCrafted;
            statDistM   = myStats.distanceTraveled / 1000;
            statPlaySec = tryGetTotalPlaytime(client);
            hasStats    = true;
        } else if (remoteStats != null) {
            statMined   = remoteStats.blocksMined;
            statPlaced  = remoteStats.blocksPlaced;
            statKilled  = remoteStats.mobsKilled;
            statCrops   = remoteStats.cropsHarvested;
            statCrafted = remoteStats.itemsCrafted;
            statDistM   = remoteStats.distanceTraveled / 1000;
            statPlaySec = remoteStats.playtimeTicks / 20;
            hasStats    = true;
        } else {
            statMined = statPlaced = statKilled = statCrops = statCrafted = statDistM = 0;
            statPlaySec = 0;
            hasStats    = false;
        }

        if (isLocal) {
            drawText(graphics, tr, "Session: " + formatDuration(myStats.sessionMs() / 1000),
                    x + PAD, y + PLAY_Y, COL_GRAY, false);
            if (statPlaySec > 0)
                drawText(graphics, tr, "Total: " + formatDuration(statPlaySec),
                        x + PAD, y + PLAY_Y + 11, 0xFFCCCCFF, false);
        } else if (hasStats && statPlaySec > 0) {
            drawText(graphics, tr, "Total: " + formatDuration(statPlaySec),
                    x + PAD, y + PLAY_Y, 0xFFCCCCFF, false);
        } else if (!hasStats) {
            String statusMsg = ServerModDetector.isPresent() ? "Syncing stats..." : "No server mod";
            drawText(graphics, tr, statusMsg,    x + PAD, y + PLAY_Y,      0xFF555577, false);
            drawText(graphics, tr, activityEmoji(act) + " " + act.displayName,
                    x + PAD, y + PLAY_Y + 11, act.color, true);
        }

        graphics.fill(x + PAD, y + DIV2_Y, x + CARD_W - PAD, y + DIV2_Y + 1, COL_DIVIDER);
        if (!hasStats) return;

        drawText(graphics, tr, "Top Badges:", x + PAD, y + BADGE_Y, COL_SECTION, true);

        String[] catNames  = {"Mining","Building","Combat","Farming","Crafting","Exploring"};
        long[]   catValues = {statMined, statPlaced, statKilled, statCrops, statCrafted, statDistM};

        List<Map.Entry<String,Achievement>> earned = new ArrayList<>();
        for (int i = 0; i < catNames.length; i++) {
            Achievement a = Achievement.forCount(catValues[i], catNames[i].toLowerCase());
            if (a != Achievement.NONE) earned.add(Map.entry(catNames[i], a));
        }
        earned.sort((a, b) -> b.getValue().ordinal() - a.getValue().ordinal());

        if (earned.isEmpty()) {
            drawText(graphics, tr, "  No badges yet", x + PAD, y + BADGE_Y + 11, COL_GRAY, false);
        } else {
            int limit = Math.min(3, earned.size());
            for (int i = 0; i < limit; i++) {
                Achievement badge = earned.get(i).getValue();
                String line = "  " + badgeIcon(badge) + " " + badge.displayName
                        + " " + earned.get(i).getKey();

                float bob = AnimationHelper.bobOffset(0.8f, 2200 + i * 650);
                drawText(graphics, tr, line, x + PAD,
                        (int)(y + BADGE_Y + 11 + i * 11 + bob), badge.color, true);
            }
        }

        graphics.fill(x + PAD, y + DIV3_Y, x + CARD_W - PAD, y + DIV3_Y + 1, COL_DIVIDER);

        drawText(graphics, tr, "Stats Progress:", x + PAD, y + STATS_Y, COL_SECTION, true);

        Integer[] order = {0, 1, 2, 3, 4, 5};
        Arrays.sort(order, (a, b) -> Long.compare(catValues[b], catValues[a]));
        int[]  barColors = {0xFFAA8855, 0xFF88AAFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFAA00, 0xFF55FFFF};
        long[] maxVals   = {10_000,     10_000,     5_000,      10_000,     10_000,     50_000};
        for (int i = 0; i < Math.min(3, order.length); i++) {
            int ci = order[i];
            drawStatBar(graphics, tr, x + PAD, y + STATS_Y + 11 + i * 16,
                    catNames[ci], catNames[ci].toLowerCase(), catValues[ci], maxVals[ci], barColors[ci]);
        }

        graphics.fill(x + PAD, y + DIV4_Y, x + CARD_W - PAD, y + DIV4_Y + 1, COL_DIVIDER);

        drawText(graphics, tr, "Recent Actions:", x + PAD, y + ACTS_Y, COL_SECTION, true);
        List<String> recent = getRecentActions(uuid,
                statMined, statPlaced, statKilled, statCrops, statCrafted);
        if (recent.isEmpty()) {
            drawText(graphics, tr, "  No activity yet",
                    x + PAD, y + ACTS_Y + 11, COL_GRAY, false);
        } else {
            int limit = Math.min(3, recent.size());
            for (int i = 0; i < limit; i++) {
                drawText(graphics, tr, "\u2022 " + recent.get(i),
                        x + PAD, y + ACTS_Y + 11 + i * 11, COL_GRAY, false);
            }
        }
    }

    private void drawStatBar(OwoUIGraphics g, TextRenderer tr, int x, int y,
                             String label, String category, long value, long max, int barColor) {
        Achievement badge = Achievement.forCount(value, category);
        drawText(g, tr, label, x, y, COL_GRAY, false);
        if (badge != Achievement.NONE)
            drawText(g, tr, badge.displayName, x + tr.getWidth(label + " "), y, badge.color, true);
        String val = formatNum(value);
        drawText(g, tr, val, x + INNER_W - tr.getWidth(val), y, COL_GRAY, false);
        int by = y + 9;
        g.fill(x, by, x + INNER_W, by + BAR_H, COL_BAR_BG);
        int fw = (int)(INNER_W * Math.min(1.0, (double) value / max));
        if (value > 0) fw = Math.max(1, fw);
        if (fw > 0)
            g.fill(x, by, x + fw, by + BAR_H,
                    AnimationHelper.withAlpha(barColor, AnimationHelper.pulseAlpha(0.85f, 3000)));
    }

    private static long tryGetTotalPlaytime(MinecraftClient client) {
        try {
            if (client.player == null) return 0;
            var sh = client.player.getStatHandler();
            if (sh == null) return 0;
            return sh.getStat(net.minecraft.stat.Stats.CUSTOM
                    .getOrCreateStat(net.minecraft.stat.Stats.PLAY_TIME)) / 20;
        } catch (Exception e) { return 0; }
    }

    public static Achievement computeTopBadge(RemoteStatCache.Entry s) {
        String[] cats = {"mining","building","combat","farming","crafting","exploring"};
        long[]   vals = {s.blocksMined, s.blocksPlaced, s.mobsKilled,
                s.cropsHarvested, s.itemsCrafted, s.distanceTraveled / 1000};
        Achievement best = Achievement.NONE;
        for (int i = 0; i < cats.length; i++) {
            Achievement a = Achievement.forCount(vals[i], cats[i]);
            if (a.ordinal() > best.ordinal()) best = a;
        }
        return best;
    }

    public static String activityEmoji(ActivityType act) {
        return switch (act) {
            case MINING    -> "\u26CF";
            case BUILDING  -> "\uD83C\uDFD7";
            case FIGHTING  -> "\u2694";
            case FARMING   -> "\uD83C\uDF3E";
            case EXPLORING -> "\uD83E\uDDED";
            case AFK       -> "\uD83D\uDCA4";
            case IDLE      -> "\u25CF";
        };
    }

    public static String badgeIcon(Achievement badge) {
        return switch (badge) {
            case BRONZE  -> "\uD83E\uDD49";
            case SILVER  -> "\uD83E\uDD48";
            case GOLD    -> "\uD83E\uDD47";
            case DIAMOND -> "\uD83D\uDC8E";
            default      -> "\u2756";
        };
    }

    private static String privacyDot(PrivacyMode p) {
        return switch (p) {
            case PUBLIC  -> "\uD83D\uDFE2";
            case FRIENDS -> "\uD83D\uDFE1";
            case GHOST   -> "\uD83D\uDD34";
        };
    }

    private static void drawText(OwoUIGraphics g, TextRenderer tr,
                                 String text, int x, int y, int color, boolean shadow) {
        g.drawText(tr, text, x, y, color, shadow);
    }

    private static void drawBorder(OwoUIGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,     y,     x+w,   y+1,   c);
        g.fill(x,     y+h-1, x+w,   y+h,   c);
        g.fill(x,     y,     x+1,   y+h,   c);
        g.fill(x+w-1, y,     x+w,   y+h,   c);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int fa=(from>>24)&0xFF, ta=(to>>24)&0xFF,
                fr=(from>>16)&0xFF, tr_=(to>>16)&0xFF,
                fg=(from>>8) &0xFF, tg=(to>>8) &0xFF,
                fb= from&0xFF,      tb= to&0xFF;
        return ((int)(fa+(ta-fa)*t)<<24)|((int)(fr+(tr_-fr)*t)<<16)
                |((int)(fg+(tg-fg)*t)<<8) |(int)(fb+(tb-fb)*t);
    }

    private static String truncate(TextRenderer tr, String s, int maxW) {
        if (tr.getWidth(s) <= maxW) return s;
        while (s.length() > 1 && tr.getWidth(s+"…") > maxW) s = s.substring(0, s.length()-1);
        return s+"…";
    }

    private static String formatNum(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    private static String formatDuration(long sec) {
        if (sec < 60)   return sec + "s";
        if (sec < 3600) return (sec / 60) + "m";
        return (sec / 3600) + "h " + ((sec % 3600) / 60) + "m";
    }

    private static String dirArrow(AbstractClientPlayerEntity from,
                                   AbstractClientPlayerEntity to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double worldCompass = Math.toDegrees(Math.atan2(dx, -dz));
        if (worldCompass < 0) worldCompass += 360;

        double observerCompass = (from.getYaw() + 180.0) % 360.0;
        if (observerCompass < 0) observerCompass += 360;

        double rel = ((worldCompass - observerCompass + 540) % 360) - 180;

        if (rel >= -22.5  && rel <  22.5)  return "\u2191";
        if (rel >=  22.5  && rel <  67.5)  return "\u2197";
        if (rel >=  67.5  && rel < 112.5)  return "\u2192";
        if (rel >= 112.5  && rel < 157.5)  return "\u2198";
        if (rel >= 157.5  || rel < -157.5) return "\u2193";
        if (rel >= -157.5 && rel < -112.5) return "\u2199";
        if (rel >= -112.5 && rel <  -67.5) return "\u2190";
        return "\u2196";
    }

    private boolean isInBounds(int mx, int my) {
        return mx >= this.x && mx < this.x + CARD_W
                && my >= this.y && my < this.y + CARD_H;
    }
}