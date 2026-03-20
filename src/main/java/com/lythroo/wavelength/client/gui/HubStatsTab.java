package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.tracking.StatTracker;
import com.lythroo.wavelength.common.data.*;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubStatsTab {

    private final HubScreen hub;

    HubStatsTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(10));
        panel.gap(5);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        MinecraftClient client = MinecraftClient.getInstance();
        StatData localStats = StatTracker.getData();

        boolean isLocalStats;
        String  viewingName;

        if (hub.selectedStatsUuid == null || client.player == null) {
            isLocalStats = true;
            viewingName  = client.player != null ? client.player.getName().getString() : "You";
        } else {
            boolean sameUuid = hub.selectedStatsUuid.equals(client.player.getUuid());
            boolean sameName = false;
            if (client.world != null) {
                for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                    if (p.getUuid().equals(hub.selectedStatsUuid)) {
                        sameName = p.getName().getString().equals(client.player.getName().getString());
                        break;
                    }
                }
            }
            isLocalStats = sameUuid || sameName;
            viewingName  = resolveUsername(hub.selectedStatsUuid, client);
        }

        int rowW = hub.panelW - 24;

        {

            long totalPlaySec = 0;
            if (isLocalStats && client.player != null) {
                RemoteStatCache.Entry selfRemote = RemoteStatCache.get(client.player.getUuid());
                if (selfRemote != null && selfRemote.playtimeTicks > 0) {
                    totalPlaySec = selfRemote.playtimeTicks / 20;
                } else {
                    try {
                        if (client.player.getStatHandler() != null) {
                            totalPlaySec = client.player.getStatHandler()
                                    .getStat(net.minecraft.stat.Stats.CUSTOM
                                            .getOrCreateStat(net.minecraft.stat.Stats.PLAY_TIME)) / 20;
                        }
                    } catch (Exception ignored) {}
                }
            }

            Achievement topBadge = isLocalStats
                    ? localStats.topBadge()
                    : HubUiHelper.computeTopBadgeFromCache(PersistentPlayerCache.get(hub.selectedStatsUuid));

            int topColor = topBadge != Achievement.NONE ? topBadge.color : 0xFF3A3A55;

            FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
            outer.surface(Surface.flat(isLocalStats ? 0xCC081408 : 0xCC0D0D18));
            outer.margins(Insets.bottom(4));

            FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(3));
            topBar.surface(Surface.flat(topColor));
            outer.child(topBar);

            FlowLayout body = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
            body.padding(Insets.of(8));
            body.gap(4);

            FlowLayout nameRow = UIContainers.horizontalFlow(Sizing.fixed(rowW - 16), Sizing.content());
            nameRow.gap(6);
            nameRow.verticalAlignment(VerticalAlignment.CENTER);

            LabelComponent nameLbl = UIComponents.label(
                    Text.literal("📊  " + (isLocalStats ? "YOUR STATS" : viewingName.toUpperCase()))
                            .withColor(isLocalStats ? 0xFFFFFFFF : 0xFFEEEEEE));
            nameLbl.shadow(true);
            nameRow.child(nameLbl);

            if (!isLocalStats) {
                nameRow.child(styledBtn(50, 14,
                        Text.literal("← Back").withColor(0xFFCCCCFF), 0xFF1E1E3A, 0xFF2E2E55,
                        () -> { hub.selectedStatsUuid = null; hub.fillContent(); }));
            }
            body.child(nameRow);

            FlowLayout badgeRow = UIContainers.horizontalFlow(Sizing.fixed(rowW - 16), Sizing.content());
            badgeRow.gap(8);
            badgeRow.verticalAlignment(VerticalAlignment.CENTER);
            if (topBadge != Achievement.NONE) {
                FlowLayout badgeChip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(14));
                badgeChip.surface(Surface.flat((topColor & 0x00FFFFFF) | 0x22000000));
                badgeChip.padding(Insets.horizontal(5));
                badgeChip.verticalAlignment(VerticalAlignment.CENTER);
                badgeChip.child(UIComponents.label(
                        Text.literal(PlayerCardComponent.badgeIcon(topBadge) + " " + topBadge.displayName)
                                .withColor(topColor)));
                badgeRow.child(badgeChip);
            }
            body.child(badgeRow);

            if (isLocalStats && (totalPlaySec > 0 || localStats.sessionMs() > 0)) {
                FlowLayout timeCard = UIContainers.verticalFlow(Sizing.fixed(rowW - 16), Sizing.content());
                timeCard.surface(Surface.flat(0xAA0A0A14));
                timeCard.padding(Insets.of(5, 8, 5, 8));
                timeCard.gap(3);
                timeCard.margins(Insets.top(2));

                if (totalPlaySec > 0) {
                    FlowLayout totalRow = UIContainers.horizontalFlow(Sizing.fixed(rowW - 32), Sizing.content());
                    totalRow.gap(6);
                    totalRow.verticalAlignment(VerticalAlignment.CENTER);
                    totalRow.child(UIComponents.label(
                            Text.literal("⏱  Total Playtime").withColor(0xFF556677)));
                    totalRow.child(UIComponents.label(
                            Text.literal(formatDuration(totalPlaySec)).withColor(0xFFCCDDEE)));
                    timeCard.child(totalRow);
                }

                long sessionSec = localStats.sessionMs() / 1000;
                if (sessionSec > 0) {
                    FlowLayout sessionRow = UIContainers.horizontalFlow(Sizing.fixed(rowW - 32), Sizing.content());
                    sessionRow.gap(6);
                    sessionRow.verticalAlignment(VerticalAlignment.CENTER);
                    sessionRow.child(UIComponents.label(
                            Text.literal("🕐  This Session").withColor(0xFF445566)));
                    sessionRow.child(UIComponents.label(
                            Text.literal(formatDuration(sessionSec)).withColor(0xFF99AABB)));
                    timeCard.child(sessionRow);
                }
                body.child(timeCard);
            }
            outer.child(body);
            panel.child(outer);
        }

        if (isLocalStats) {
            buildLocalCategoryCards(panel, localStats, rowW);
        } else {
            buildRemoteCards(panel, viewingName, rowW, client);
        }

        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private void buildLocalCategoryCards(FlowLayout panel, StatData stats, int rowW) {
        panel.child(sectionHeader("✦  ACHIEVEMENTS & PROGRESS"));
        panel.child(divider(hub.panelW));
        panel.child(spacer(2));
        for (int i = 0; i < CATS.length; i++) {
            panel.child(buildStatCard(stats, CATS[i], LABELS[i], COLORS[i], rowW));
            panel.child(spacer(2));
        }
    }

    private FlowLayout buildStatCard(StatData stats, String category, String label,
                                     int barColor, int rowW) {
        Achievement badge    = stats.badgeForCategory(category);
        long        count    = stats.countForCategory(category);
        float       progress = badge.progressToNext(count, category);
        int         nextT    = badge.nextThresholdFor(category);
        int         accentColor = badge != Achievement.NONE ? badge.color : barColor;

        int cardBg = badge != Achievement.NONE
                ? ((badge.color >> 16 & 0xFF) / 12 << 16
                |  (badge.color >>  8 & 0xFF) / 12 <<  8
                |  (badge.color       & 0xFF) / 12
                | 0xCC000000)
                : 0xCC0D0D18;

        FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
        outer.surface(Surface.flat(cardBg));
        outer.margins(Insets.vertical(1));

        FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(3));
        topBar.surface(Surface.flat(accentColor));
        outer.child(topBar);

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
        card.padding(Insets.of(6, 8, 6, 8));
        card.gap(3);
        int innerW = rowW - 16;

        FlowLayout headerRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
        headerRow.gap(6);
        headerRow.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent catLbl = UIComponents.label(Text.literal(label).withColor(barColor));
        catLbl.shadow(true);
        headerRow.child(catLbl);

        if (badge != Achievement.NONE) {
            FlowLayout badgeChip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(13));
            badgeChip.surface(Surface.flat((badge.color & 0x00FFFFFF) | 0x22000000));
            badgeChip.padding(Insets.horizontal(5));
            badgeChip.verticalAlignment(VerticalAlignment.CENTER);
            badgeChip.child(UIComponents.label(
                    Text.literal(PlayerCardComponent.badgeIcon(badge) + " " + badge.displayName)
                            .withColor(badge.color)));
            headerRow.child(badgeChip);
        }

        LabelComponent countLbl = UIComponents.label(
                Text.literal(formatNum(count)).withColor(0xFF888888));
        headerRow.child(countLbl);
        card.child(headerRow);

        if (badge != Achievement.NONE) {
            card.child(UIComponents.label(
                    Text.literal("  " + badge.flavorText)
                            .withColor((badge.color & 0x00FFFFFF) | 0x77000000)));
        }

        if (nextT > 0) {
            int fw = Math.max(0, (int)(innerW * progress));
            FlowLayout barTrack = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(3));
            barTrack.surface(Surface.flat(COL_BAR_BG));
            if (fw > 0) {
                FlowLayout fill = UIContainers.horizontalFlow(Sizing.fixed(fw), Sizing.fixed(3));
                fill.surface(Surface.flat(barColor));
                barTrack.child(fill);
            }
            card.child(barTrack);

            Achievement next = Achievement.values()[badge.ordinal() + 1];
            FlowLayout hintRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
            hintRow.gap(4);
            hintRow.verticalAlignment(VerticalAlignment.CENTER);
            hintRow.child(UIComponents.label(
                    Text.literal((int)(progress * 100) + "%").withColor(0xFF555566)));
            hintRow.child(UIComponents.label(
                    Text.literal("→ " + next.medal + " " + next.displayName
                            + " at " + formatNum(nextT)).withColor(0xFF444455)));
            card.child(hintRow);
        } else {
            card.child(UIComponents.label(
                    Text.literal("◆ Diamond — maximum tier reached")
                            .withColor(Achievement.DIAMOND.color)));
        }

        outer.child(card);
        return outer;
    }

    private void buildRemoteCards(FlowLayout panel, String viewingName,
                                  int rowW, MinecraftClient client) {
        RemoteStatCache.Entry remoteStats = RemoteStatCache.get(hub.selectedStatsUuid);
        PersistentPlayerCache.Entry cachedOffline = remoteStats == null
                ? PersistentPlayerCache.get(hub.selectedStatsUuid) : null;

        if (remoteStats == null && cachedOffline == null) {
            panel.child(UIComponents.label(
                    Text.literal("No stat data for " + viewingName + ".").withColor(COL_GRAY)));
            panel.child(UIComponents.label(
                    Text.literal("Stats sync requires the Wavelength server mod.").withColor(0xFF555577)));
            return;
        }

        boolean isOffline = remoteStats == null;
        if (isOffline) {
            panel.child(UIComponents.label(
                    Text.literal("Showing cached stats — player is offline").withColor(0xFF555566)));
            panel.child(spacer(2));
        }

        panel.child(sectionHeader((isOffline ? "⌛  " : "✦  ") + viewingName.toUpperCase() + " — STATS"));
        panel.child(divider(hub.panelW));
        panel.child(spacer(2));

        long[] vals;
        long playtime;
        if (remoteStats != null) {
            vals = new long[]{ remoteStats.blocksMined, remoteStats.blocksPlaced,
                    remoteStats.mobsKilled, remoteStats.cropsHarvested,
                    remoteStats.itemsCrafted, remoteStats.distanceTraveled / 1000 };
            playtime = remoteStats.playtimeTicks;
        } else {
            vals = new long[]{ cachedOffline.blocksMined, cachedOffline.blocksPlaced,
                    cachedOffline.mobsKilled, cachedOffline.cropsHarvested,
                    cachedOffline.itemsCrafted, cachedOffline.distanceTraveled / 1000 };
            playtime = cachedOffline.playtimeTicks;
        }

        for (int i = 0; i < CATS.length; i++) {
            panel.child(buildRemoteStatCard(CATS[i], LABELS[i], COLORS[i], vals[i], rowW));
            panel.child(spacer(2));
        }

        if (playtime > 0) {
            panel.child(spacer(4));
            FlowLayout timeCard = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
            timeCard.surface(Surface.flat(0xCC0D0D18));
            timeCard.margins(Insets.vertical(1));

            FlowLayout timeBar = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(3));
            timeBar.surface(Surface.flat(0xFF556677));
            timeCard.child(timeBar);

            FlowLayout timeBody = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
            timeBody.padding(Insets.of(6, 8, 6, 8));
            timeBody.gap(3);

            FlowLayout totalRow = UIContainers.horizontalFlow(Sizing.fixed(rowW - 16), Sizing.content());
            totalRow.gap(6);
            totalRow.verticalAlignment(VerticalAlignment.CENTER);
            totalRow.child(UIComponents.label(
                    Text.literal("⏱  Total Playtime").withColor(0xFF556677)));
            totalRow.child(UIComponents.label(
                    Text.literal(formatDuration(playtime / 20)).withColor(0xFFCCDDEE)));
            timeBody.child(totalRow);

            timeCard.child(timeBody);
            panel.child(timeCard);
        }
    }

    private FlowLayout buildRemoteStatCard(String category, String label,
                                           int barColor, long count, int rowW) {
        Achievement badge = Achievement.forCount(count, category);
        int accentColor   = badge != Achievement.NONE ? badge.color : barColor;

        FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
        outer.surface(Surface.flat(0xCC0D0D18));
        outer.margins(Insets.vertical(1));

        FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(3));
        topBar.surface(Surface.flat(accentColor));
        outer.child(topBar);

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
        card.padding(Insets.of(6, 8, 4, 8));
        card.gap(3);
        int innerW = rowW - 16;

        FlowLayout headerRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
        headerRow.gap(6);
        headerRow.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent catLbl = UIComponents.label(Text.literal(label).withColor(barColor));
        catLbl.shadow(true);
        headerRow.child(catLbl);

        if (badge != Achievement.NONE) {
            FlowLayout badgeChip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(13));
            badgeChip.surface(Surface.flat((badge.color & 0x00FFFFFF) | 0x22000000));
            badgeChip.padding(Insets.horizontal(5));
            badgeChip.verticalAlignment(VerticalAlignment.CENTER);
            badgeChip.child(UIComponents.label(
                    Text.literal(PlayerCardComponent.badgeIcon(badge) + " " + badge.displayName)
                            .withColor(badge.color)));
            headerRow.child(badgeChip);
        }

        LabelComponent countLbl = UIComponents.label(
                Text.literal(category.equals("exploring") ? formatDist(count * 1000) : formatNum(count))
                        .withColor(0xFF888888));
        headerRow.child(countLbl);
        card.child(headerRow);

        long threshold = Achievement.DIAMOND.thresholdFor(category);
        if (threshold > 0) {
            float pct   = Math.min(1f, (float) count / threshold);
            int   fillW = (int)(innerW * pct);
            FlowLayout barTrack = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(3));
            barTrack.surface(Surface.flat(COL_BAR_BG));
            if (fillW > 0) {
                FlowLayout fill = UIContainers.horizontalFlow(Sizing.fixed(fillW), Sizing.fixed(3));
                fill.surface(Surface.flat((barColor & 0x00FFFFFF) | 0x88000000));
                barTrack.child(fill);
            }
            card.child(barTrack);
        }

        outer.child(card);
        return outer;
    }

    private FlowLayout buildSimpleCard(String label, String value, int accentColor, int rowW) {
        FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(rowW), Sizing.content());
        outer.surface(Surface.flat(0xCC0D0D18));

        FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(3));
        topBar.surface(Surface.flat(accentColor));
        outer.child(topBar);

        FlowLayout body = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.content());
        body.padding(Insets.of(6, 8, 6, 8));
        body.gap(6);
        body.verticalAlignment(VerticalAlignment.CENTER);
        body.child(UIComponents.label(Text.literal(label).withColor(accentColor)));
        body.child(UIComponents.label(Text.literal(value).withColor(0xFFCCCCDD)));
        outer.child(body);
        return outer;
    }
}