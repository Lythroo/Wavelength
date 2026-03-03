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
        panel.padding(Insets.of(16));
        panel.gap(6);
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

        panel.child(UIComponents.label(
                Text.literal(isLocalStats ? "📊 YOUR STATS" : "📊 STATS: " + viewingName)
                        .withColor(COL_HEADER_TXT)));

        if (!isLocalStats) {
            panel.child(styledBtn(60, 14,
                    Text.literal("← Back").withColor(0xFFCCCCFF), 0xFF1E1E3A, 0xFF2E2E55,
                    () -> { hub.selectedStatsUuid = null; hub.fillContent(); }));
        }

        panel.child(divider(hub.panelW));

        if (!isLocalStats) {
            buildRemoteStats(panel, viewingName, client);
            return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
        }

        long totalPlaySec = 0;
        try {
            if (client.player != null && client.player.getStatHandler() != null) {
                totalPlaySec = client.player.getStatHandler()
                        .getStat(net.minecraft.stat.Stats.CUSTOM
                                .getOrCreateStat(net.minecraft.stat.Stats.PLAY_TIME)) / 20;
            }
        } catch (Exception ignored) {}

        panel.child(sectionHeader("⏱ PLAYTIME"));
        panel.child(divider(hub.panelW));
        if (totalPlaySec > 0) {
            panel.child(UIComponents.label(
                    Text.literal("Total on server: " + formatDuration(totalPlaySec)).withColor(0xFFCCCCFF)));
        }
        panel.child(UIComponents.label(
                Text.literal("This session: " + formatDuration(localStats.sessionMs() / 1000)).withColor(COL_GRAY)));
        panel.child(spacer(8));

        panel.child(sectionHeader("✦ YOUR ACHIEVEMENTS"));
        panel.child(divider(hub.panelW));
        for (int i = 0; i < CATS.length; i++) {
            panel.child(buildStatRow(localStats, CATS[i], LABELS[i], COLORS[i]));
        }

        Achievement top = localStats.topBadge();
        if (top != Achievement.NONE) {
            panel.child(divider(hub.panelW));
            panel.child(UIComponents.label(
                    Text.literal(PlayerCardComponent.badgeIcon(top) + " Top badge: " + top.displayName)
                            .withColor(top.color)));
        }
        panel.child(spacer(10));

        panel.child(sectionHeader("📊 TOTALS"));
        panel.child(divider(hub.panelW));
        panel.child(statLine("Blocks mined",      formatNum(localStats.blocksMined),      hub.panelW));
        panel.child(statLine("Blocks placed",     formatNum(localStats.blocksPlaced),     hub.panelW));
        panel.child(statLine("Kills",             formatNum(localStats.mobsKilled),       hub.panelW));
        panel.child(statLine("Items crafted",     formatNum(localStats.itemsCrafted),     hub.panelW));
        panel.child(statLine("Distance traveled", formatDist(localStats.distanceTraveled), hub.panelW));
        if (totalPlaySec > 0) {
            panel.child(statLine("Total playtime", formatDuration(totalPlaySec), hub.panelW));
        }
        panel.child(spacer(16));

        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private void buildRemoteStats(FlowLayout panel, String viewingName, MinecraftClient client) {
        RemoteStatCache.Entry remoteStats = RemoteStatCache.get(hub.selectedStatsUuid);
        PersistentPlayerCache.Entry cachedOffline = (remoteStats == null)
                ? PersistentPlayerCache.get(hub.selectedStatsUuid) : null;

        if (remoteStats == null && cachedOffline == null) {
            panel.child(UIComponents.label(
                    Text.literal("No stat data received for " + viewingName + ".").withColor(COL_GRAY)));
            panel.child(UIComponents.label(
                    Text.literal("Stats sync requires the Wavelength server mod.").withColor(0xFF555577)));
            return;
        }

        if (remoteStats == null) {

            panel.child(UIComponents.label(
                    Text.literal("(Showing last known stats — player is offline)").withColor(0xFF666688)));
            panel.child(spacer(4));
            panel.child(sectionHeader("\u2756 " + viewingName.toUpperCase() + " (CACHED)"));
            panel.child(divider(hub.panelW));
            panel.child(statLine("Blocks mined",      formatNum(cachedOffline.blocksMined),      hub.panelW));
            panel.child(statLine("Blocks placed",     formatNum(cachedOffline.blocksPlaced),     hub.panelW));
            panel.child(statLine("Kills",             formatNum(cachedOffline.mobsKilled),       hub.panelW));
            panel.child(statLine("Items crafted",     formatNum(cachedOffline.itemsCrafted),     hub.panelW));
            panel.child(statLine("Distance traveled", formatDist(cachedOffline.distanceTraveled), hub.panelW));
            if (cachedOffline.playtimeTicks > 0) {
                panel.child(statLine("Total playtime", formatDuration(cachedOffline.playtimeTicks / 20), hub.panelW));
            }
            panel.child(spacer(10));
            panel.child(sectionHeader("\u2756 ACHIEVEMENTS"));
            panel.child(divider(hub.panelW));
            long[] offVals = {cachedOffline.blocksMined, cachedOffline.blocksPlaced,
                    cachedOffline.mobsKilled, cachedOffline.cropsHarvested,
                    cachedOffline.itemsCrafted, cachedOffline.distanceTraveled / 1000};
            for (int i = 0; i < CATS.length; i++) {
                Achievement a = Achievement.forCount(offVals[i], CATS[i]);
                if (a != Achievement.NONE) {
                    String catLabel = CATS[i].substring(0, 1).toUpperCase() + CATS[i].substring(1);
                    panel.child(UIComponents.label(
                            Text.literal(PlayerCardComponent.badgeIcon(a) + " " + a.displayName
                                            + " " + catLabel + "  (" + formatNum(offVals[i]) + ")")
                                    .withColor(a.color)));
                }
            }
        } else {

            panel.child(sectionHeader("✦ " + viewingName.toUpperCase() + "'S STATS"));
            panel.child(divider(hub.panelW));
            panel.child(statLine("Blocks mined",      formatNum(remoteStats.blocksMined),      hub.panelW));
            panel.child(statLine("Blocks placed",     formatNum(remoteStats.blocksPlaced),     hub.panelW));
            panel.child(statLine("Kills",             formatNum(remoteStats.mobsKilled),       hub.panelW));
            panel.child(statLine("Items crafted",     formatNum(remoteStats.itemsCrafted),     hub.panelW));
            panel.child(statLine("Distance traveled", formatDist(remoteStats.distanceTraveled), hub.panelW));
            if (remoteStats.playtimeTicks > 0) {
                panel.child(statLine("Total playtime", formatDuration(remoteStats.playtimeTicks / 20), hub.panelW));
            }
            panel.child(spacer(10));
            panel.child(sectionHeader("✦ ACHIEVEMENTS"));
            panel.child(divider(hub.panelW));
            panel.child(statLine("⛏ Mining",   formatNum(remoteStats.blocksMined),  hub.panelW));
            panel.child(statLine("🏗 Building", formatNum(remoteStats.blocksPlaced), hub.panelW));
            panel.child(statLine("⚔ Combat",   formatNum(remoteStats.mobsKilled) + " kills", hub.panelW));
            panel.child(statLine("🔨 Crafting", formatNum(remoteStats.itemsCrafted), hub.panelW));
        }
    }

    FlowLayout buildStatRow(StatData stats, String category, String label, int barColor) {
        Achievement badge    = stats.badgeForCategory(category);
        long        count    = stats.countForCategory(category);
        float       progress = badge.progressToNext(count, category);
        int         nextT    = badge.nextThresholdFor(category);

        FlowLayout row = UIContainers.verticalFlow(Sizing.fixed(hub.panelW - 32), Sizing.content());
        row.padding(Insets.of(5));
        row.gap(3);
        if (badge != Achievement.NONE) {
            int tierBg = ((badge.color >> 16 & 0xFF) / 8 << 16)
                    | ((badge.color >>  8 & 0xFF) / 8 <<  8)
                    |  (badge.color        & 0xFF) / 8
                    | 0x20000000;
            row.surface(Surface.flat(tierBg));
        }

        FlowLayout topLine = UIContainers.horizontalFlow(Sizing.fixed(hub.panelW - 44), Sizing.content());
        topLine.gap(6);
        topLine.verticalAlignment(VerticalAlignment.CENTER);
        topLine.child(UIComponents.label(Text.literal(label).withColor(barColor)));
        if (badge != Achievement.NONE) {
            topLine.child(UIComponents.label(
                    Text.literal(PlayerCardComponent.badgeIcon(badge) + " " + badge.displayName)
                            .withColor(badge.color)));
        }
        topLine.child(UIComponents.label(Text.literal("(" + formatNum(count) + ")").withColor(COL_GRAY)));
        row.child(topLine);

        if (badge != Achievement.NONE) {
            row.child(UIComponents.label(
                    Text.literal("  " + badge.flavorText)
                            .withColor((badge.color & 0x00FFFFFF) | 0x99000000)));
        }

        if (nextT > 0) {
            FlowLayout barRow = UIContainers.horizontalFlow(Sizing.fixed(hub.panelW - 44), Sizing.fixed(10));
            barRow.gap(6);
            barRow.verticalAlignment(VerticalAlignment.CENTER);
            int barTrackW = hub.panelW - 150;
            FlowLayout track = UIContainers.horizontalFlow(Sizing.fixed(barTrackW), Sizing.fixed(6));
            track.surface(Surface.flat(COL_BAR_BG));
            int fw = (int)(barTrackW * progress);
            if (fw > 0) {
                FlowLayout fill = UIContainers.horizontalFlow(Sizing.fixed(fw), Sizing.fixed(6));
                fill.surface(Surface.flat(barColor));
                track.child(fill);
            }
            barRow.child(track);
            barRow.child(UIComponents.label(
                    Text.literal((int)(progress * 100) + "% → " + formatNum(nextT)).withColor(COL_GRAY)));
            row.child(barRow);

            Achievement next = Achievement.values()[badge.ordinal() + 1];
            row.child(UIComponents.label(
                    Text.literal("  Next: " + next.medal + " " + next.displayName
                            + " at " + formatNum(nextT)).withColor(0xFF555577)));
        } else {
            row.child(UIComponents.label(
                    Text.literal("  \u25C6 MAX TIER — DIAMOND ACHIEVED \u25C6")
                            .withColor(Achievement.DIAMOND.color)));
        }
        return row;
    }
}