package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.client.tracking.StatTracker;
import com.lythroo.wavelength.common.data.*;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubLeaderboardTab {

    record AllStats(long mined, long placed, long killed, long farmed, long crafted, long distM) {}

    record LeaderboardEntry(String name, UUID uuid, long value, boolean isLocal, AllStats all) {}

    private final HubScreen hub;

    HubLeaderboardTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(10));
        panel.gap(5);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            panel.child(UIComponents.label(Text.literal("Not in a world.").formatted(Formatting.GRAY)));
            return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
        }

        int rowW   = hub.panelW - 24;
        int btnGap = 4;
        int btnW   = (rowW - btnGap * 2) / 3;

        for (int row = 0; row < 2; row++) {
            FlowLayout catRow = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.content());
            catRow.gap(btnGap);
            for (int col = 0; col < 3; col++) {
                int i = row * 3 + col;
                final String key  = CATS[i];
                final String lbl  = SHORT[i];
                final int    col_ = COLORS[i];
                boolean active = key.equals(hub.leaderboardCat);
                int normalBg = active ? 0xFF252555 : 0xFF141424;
                int hoverBg  = active ? 0xFF353575 : 0xFF202044;
                int textColor = active ? col_ : 0xFF6666AA;
                catRow.child(styledBtn(btnW, 20,
                        Text.literal(active ? "● " + lbl : lbl).withColor(textColor),
                        normalBg, hoverBg,
                        () -> { hub.leaderboardCat = key; hub.fillContent(); }));
            }
            panel.child(catRow);
        }

        panel.child(spacer(2));

        int    catIdx   = Arrays.asList(CATS).indexOf(hub.leaderboardCat);
        String catLabel = LABELS[catIdx];
        int    catColor = COLORS[catIdx];

        panel.child(sectionHeader("🏆  " + catLabel.toUpperCase() + "  LEADERBOARD"));
        panel.child(divider(hub.panelW));

        boolean isSingleplayer = client.getServer() != null;
        List<LeaderboardEntry> entries = collectData(hub.leaderboardCat, client, isSingleplayer);
        UUID localUuid = client.player.getUuid();

        int localRank = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).uuid().equals(localUuid)) { localRank = i + 1; break; }
        }

        if (entries.size() <= 1) {
            panel.child(UIComponents.label(Text.literal(isSingleplayer
                    ? "Singleplayer — no other players to compare."
                    : "No stat data yet — requires the Wavelength server mod.").withColor(COL_GRAY)));
        }

        Set<Integer> showSet = new LinkedHashSet<>();
        int topRows = Math.min(5, entries.size());
        for (int i = 0; i < topRows; i++) showSet.add(i);
        if (localRank > 0) {
            for (int i = Math.max(topRows, localRank - 2); i < Math.min(entries.size(), localRank + 1); i++)
                showSet.add(i);
        }
        if (entries.size() <= 12) {
            showSet.clear();
            for (int i = 0; i < entries.size(); i++) showSet.add(i);
        }

        int prevIdx = -2;
        for (int idx : showSet) {
            if (idx - prevIdx > 1 && prevIdx >= 0) {
                FlowLayout gap = UIContainers.horizontalFlow(Sizing.fixed(rowW), Sizing.fixed(14));
                gap.horizontalAlignment(HorizontalAlignment.CENTER);
                gap.child(UIComponents.label(Text.literal("· · ·").withColor(0xFF333355)));
                panel.child(gap);
            }
            prevIdx = idx;
            panel.child(buildCard(idx + 1, entries.get(idx), catColor, catIdx, localUuid));
        }

        panel.child(divider(hub.panelW));
        if (localRank == 1 && entries.size() > 1) {
            panel.child(UIComponents.label(
                    Text.literal("🏆  You're #1 in " + catLabel + "!  Keep it up!").withColor(COL_GOLD)));
        } else if (localRank > 1 && localRank <= entries.size()) {
            long gap = entries.get(localRank - 2).value() - entries.get(localRank - 1).value();
            panel.child(UIComponents.label(
                    Text.literal("💪  " + formatLeaderboardValue(gap, hub.leaderboardCat)
                                    + " to reach rank " + (localRank - 1) + "!")
                            .withColor(0xFF88FF88)));
        } else if (localRank == 1 && entries.size() == 1) {
            panel.child(UIComponents.label(
                    Text.literal("📊  Get friends on the server to compare stats!").withColor(COL_GRAY)));
        }

        if (!isSingleplayer && entries.size() > 1) {
            panel.child(UIComponents.label(
                    Text.literal(entries.size() + " players tracked  •  data updates every session")
                            .withColor(0xFF2A2A44)));
        }

        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private FlowLayout buildCard(int rank, LeaderboardEntry e,
                                 int catColor, int catIdx, UUID localUuid) {
        boolean isLocal  = e.uuid().equals(localUuid);
        int     cardRowW = hub.panelW - 24;

        int rankAccent;
        if      (rank == 1) rankAccent = 0xFFFFD700;
        else if (rank == 2) rankAccent = 0xFFC0C0C0;
        else if (rank == 3) rankAccent = 0xFFCD7F32;
        else                rankAccent = 0xFF3A3A55;

        int cardBg = isLocal ? 0xCC081408 : 0xCC0D0D18;

        FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(cardRowW), Sizing.content());
        outer.surface(Surface.flat(cardBg));
        outer.margins(Insets.vertical(2));

        FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fixed(cardRowW), Sizing.fixed(3));
        topBar.surface(Surface.flat(rankAccent));
        outer.child(topBar);

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(cardRowW), Sizing.content());
        card.padding(Insets.of(5, 8, 5, 8));
        card.gap(4);
        outer.child(card);

        int innerW = cardRowW - 16;

        FlowLayout headerRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
        headerRow.gap(6);
        headerRow.verticalAlignment(VerticalAlignment.CENTER);

        String rankStr = rank <= 3
                ? new String[]{"", "1ST", "2ND", "3RD"}[rank]
                : "#" + rank;
        FlowLayout rankBlock = UIContainers.horizontalFlow(Sizing.fixed(34), Sizing.fixed(18));
        rankBlock.surface(Surface.flat((rankAccent & 0x00FFFFFF) | 0x22000000));
        rankBlock.horizontalAlignment(HorizontalAlignment.CENTER);
        rankBlock.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent rankLbl = UIComponents.label(Text.literal(rankStr).withColor(rankAccent));
        rankLbl.shadow(true);
        rankBlock.child(rankLbl);
        headerRow.child(rankBlock);

        FlowLayout nameGroup = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nameGroup.gap(5);
        nameGroup.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent nameLbl = UIComponents.label(
                Text.literal(e.name())
                        .withColor(rank == 1 ? 0xFFFFFFFF : rank <= 3 ? 0xFFEEEEEE : 0xFFBBBBCC));
        nameLbl.shadow(rank <= 3);
        nameGroup.child(nameLbl);
        if (isLocal) {
            FlowLayout youBadge = UIContainers.horizontalFlow(Sizing.fixed(30), Sizing.fixed(12));
            youBadge.surface(Surface.flat(0xFF1A4A1A));
            youBadge.horizontalAlignment(HorizontalAlignment.CENTER);
            youBadge.verticalAlignment(VerticalAlignment.CENTER);
            LabelComponent youLbl = UIComponents.label(Text.literal("YOU").withColor(0xFF55FF55));
            youLbl.shadow(true);
            youBadge.child(youLbl);
            nameGroup.child(youBadge);
        }
        headerRow.child(nameGroup);

        LabelComponent valLbl = UIComponents.label(
                Text.literal(formatLeaderboardValue(e.value(), hub.leaderboardCat))
                        .withColor(catColor));
        valLbl.shadow(true);
        headerRow.child(valLbl);

        card.child(headerRow);

        String[] icons = {"⛏","🏗","⚔","🌾","🔨","🧭"};
        long[]   vals  = allStatsArray(e.all());
        int      colW  = innerW / 3;

        for (int gridRow = 0; gridRow < 2; gridRow++) {
            FlowLayout miniRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
            miniRow.gap(0);
            for (int col = 0; col < 3; col++) {
                int i = gridRow * 3 + col;
                boolean isPrimary = (i == catIdx);
                FlowLayout cell = UIContainers.horizontalFlow(Sizing.fixed(colW), Sizing.content());
                cell.padding(Insets.of(1, 2, 1, 0));
                cell.gap(3);
                cell.verticalAlignment(VerticalAlignment.CENTER);

                cell.child(UIComponents.label(
                        Text.literal(icons[i]).withColor(isPrimary ? COLORS[i] : 0xFF384050)));

                cell.child(UIComponents.label(
                        Text.literal(formatLeaderboardValue(vals[i], CATS[i]))
                                .withColor(isPrimary ? 0xFFAAAAAA : 0xFF505060)));
                miniRow.child(cell);
            }
            card.child(miniRow);
        }

        long  threshold = Achievement.DIAMOND.thresholdFor(CATS[catIdx]);
        float barFill   = threshold > 0 ? Math.min(1f, (float) Math.min(vals[catIdx], threshold) / threshold) : 0f;
        int   barFillW  = (int)(innerW * barFill);

        FlowLayout barTrack = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(2));
        barTrack.surface(Surface.flat(0xFF1A1A28));
        if (barFillW > 0) {
            int barColor = (catColor & 0x00FFFFFF) | 0x99000000;
            FlowLayout barFillLayout = UIContainers.horizontalFlow(Sizing.fixed(barFillW), Sizing.fixed(2));
            barFillLayout.surface(Surface.flat(barColor));
            barTrack.child(barFillLayout);
        }
        card.child(barTrack);

        return outer;
    }

    private List<LeaderboardEntry> collectData(String cat, MinecraftClient client, boolean isSingleplayer) {
        Map<UUID, LeaderboardEntry> map = new LinkedHashMap<>();

        if (client.player != null) {
            UUID uuid = client.player.getUuid();
            StatData sd = StatTracker.getData();
            AllStats all = new AllStats(sd.blocksMined, sd.blocksPlaced, sd.mobsKilled,
                    sd.cropsHarvested, sd.itemsCrafted, sd.distanceTraveled / 1000);
            map.put(uuid, new LeaderboardEntry(
                    client.player.getName().getString(), uuid, sd.countForCategory(cat), true, all));
        }

        if (!isSingleplayer && client.player != null) {
            UUID localUuid = client.player.getUuid();
            for (Map.Entry<UUID, PersistentPlayerCache.Entry> e :
                    PersistentPlayerCache.getAllExcept(localUuid)) {
                UUID uuid = e.getKey();
                PersistentPlayerCache.Entry pc = e.getValue();
                if (pc.username.isBlank()) continue;

                RemoteStatCache.Entry live = RemoteStatCache.get(uuid);
                AllStats all;
                long val;
                if (live != null) {
                    all = new AllStats(live.blocksMined, live.blocksPlaced, live.mobsKilled,
                            live.cropsHarvested, live.itemsCrafted, live.distanceTraveled / 1000);
                    val = switch (cat) {
                        case "mining"    -> live.blocksMined;    case "building"  -> live.blocksPlaced;
                        case "combat"    -> live.mobsKilled;     case "farming"   -> live.cropsHarvested;
                        case "crafting"  -> live.itemsCrafted;   case "exploring" -> live.distanceTraveled / 1000;
                        default -> 0L;
                    };
                } else {
                    all = new AllStats(pc.blocksMined, pc.blocksPlaced, pc.mobsKilled,
                            pc.cropsHarvested, pc.itemsCrafted, pc.distanceTraveled / 1000);
                    val = switch (cat) {
                        case "mining"    -> pc.blocksMined;    case "building"  -> pc.blocksPlaced;
                        case "combat"    -> pc.mobsKilled;     case "farming"   -> pc.cropsHarvested;
                        case "crafting"  -> pc.itemsCrafted;   case "exploring" -> pc.distanceTraveled / 1000;
                        default -> 0L;
                    };
                }
                map.put(uuid, new LeaderboardEntry(pc.username, uuid, val, false, all));
            }
        }

        return map.values().stream()
                .sorted((a, b) -> Long.compare(b.value(), a.value()))
                .collect(Collectors.toList());
    }

    static long[] allStatsArray(AllStats s) {
        if (s == null) return new long[6];
        return new long[]{s.mined(), s.placed(), s.killed(), s.farmed(), s.crafted(), s.distM()};
    }

    static Achievement topBadgeFromAllStats(AllStats s) {
        if (s == null) return Achievement.NONE;
        Achievement best = Achievement.NONE;
        long[] vals = allStatsArray(s);
        for (int i = 0; i < CATS.length; i++) {
            Achievement a = Achievement.forCount(vals[i], CATS[i]);
            if (a.ordinal() > best.ordinal()) best = a;
        }
        return best;
    }
}