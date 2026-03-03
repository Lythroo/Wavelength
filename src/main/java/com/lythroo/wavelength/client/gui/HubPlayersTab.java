package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.WavelengthClient;
import com.lythroo.wavelength.config.WavelengthConfig;
import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.ClientProjectCache;
import com.lythroo.wavelength.common.data.ProjectData;
import com.lythroo.wavelength.common.network.ServerModDetector;
import com.lythroo.wavelength.common.network.SyncPacket;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubPlayersTab {

    private final HubScreen hub;

    HubPlayersTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        FlowLayout outer = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        outer.gap(0);

        FlowLayout topBar = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        topBar.surface(Surface.flat(0xCC07071A));
        topBar.verticalAlignment(VerticalAlignment.CENTER);
        topBar.padding(Insets.of(4, 0, 4, 8));
        topBar.gap(6);

        LabelComponent searchIcon = UIComponents.label(Text.literal("🔍").withColor(0xFF7788AA));
        searchIcon.shadow(true);
        topBar.child(searchIcon);

        int sortBtnsW = 3 * 72 + 2 * 4 + 34;
        int searchFieldW = Math.max(60, hub.width - sortBtnsW - 50);

        TextBoxComponent searchField = UIComponents.textBox(Sizing.fixed(searchFieldW));
        searchField.setMaxLength(40);
        searchField.setText(hub.searchQuery);
        searchField.setPlaceholder(Text.literal("Search players…").withColor(0xFF445566));
        searchField.onChanged().subscribe(q -> {
            hub.searchQuery = q.trim().toLowerCase();
            if (hub.playersGridRef != null) {
                hub.playersGridRef.clearChildren();
                fillGrid(hub.playersGridRef);
            }
        });
        topBar.child(searchField);
        topBar.child(UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(1)));

        topBar.child(UIComponents.label(Text.literal("Sort:").withColor(COL_GRAY)));
        for (HubScreen.SortMode m : HubScreen.SortMode.values()) {
            boolean active = (m == hub.sortMode);
            int normalBg  = active ? COL_SORT_ACTIVE : COL_SORT_IDLE;
            int hoverBg   = active ? COL_SORT_ACTIVE : 0xFF3A3A5A;
            int textColor = active ? 0xFFDDDDFF : 0xFF888899;
            String sortStr = switch (m) {
                case DISTANCE -> "Distance"; case ACTIVITY -> "Activity"; case NAME -> "Name";
            };
            Text lbl = Text.literal(active ? "▶ " + sortStr : sortStr).withColor(textColor);
            HubScreen.SortMode mRef = m;
            topBar.child(styledBtn(72, 16, lbl, normalBg, hoverBg,
                    () -> { hub.sortMode = mRef; hub.fillContent(); }));
        }
        outer.child(topBar);

        FlowLayout grid = UIContainers.ltrTextFlow(Sizing.fill(100), Sizing.content());
        grid.padding(Insets.of(10));
        grid.gap(6);
        hub.playersGridRef = grid;
        fillGrid(grid);

        hub.playersScrollRef = UIContainers.verticalScroll(Sizing.fill(100),
                Sizing.fixed(hub.contentH - 26), grid);
        outer.child(hub.playersScrollRef);
        return outer;
    }

    void fillGrid(FlowLayout grid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            grid.child(UIComponents.label(Text.literal("Not in a world.").formatted(Formatting.GRAY)));
            return;
        }

        AbstractClientPlayerEntity me = client.player;

        Comparator<AbstractClientPlayerEntity> cmp = switch (hub.sortMode) {
            case DISTANCE -> Comparator.comparingDouble(p -> p.squaredDistanceTo(me));
            case ACTIVITY -> Comparator.comparing(p -> ActivityDetector.getActivity(p.getUuid()).displayName);
            case NAME     -> Comparator.comparing(p -> p.getName().getString());
        };

        List<AbstractClientPlayerEntity> players = client.world.getPlayers().stream()
                .filter(p -> matches(p.getName().getString()))
                .sorted(cmp)
                .toList();

        List<UUID> onlineUuids = client.world.getPlayers().stream()
                .map(AbstractClientPlayerEntity::getUuid).toList();

        for (AbstractClientPlayerEntity p : players) {
            final UUID   cardUuid = p.getUuid();
            final String cardName = p.getName().getString();

            FlowLayout cardWrap = UIContainers.verticalFlow(
                    Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.content());
            cardWrap.gap(0);
            cardWrap.margins(Insets.of(4));

            PlayerCardComponent cardComp = new PlayerCardComponent(p);
            cardComp.mouseDown().subscribe((mouseX, mouseY) -> {
                playClick();
                MinecraftClient.getInstance().execute(() -> {
                    hub.selectedStatsUuid = cardUuid;
                    hub.activeTab = HubScreen.Tab.STATS;
                    hub.fillTabBar();
                    hub.fillContent();
                });
                return true;
            });
            cardWrap.child(cardComp);

            boolean isLocal = cardUuid.equals(me.getUuid()) || cardName.equals(me.getName().getString());
            if (isLocal) {
                buildLocalControls(cardWrap);
            } else {
                buildFriendButtons(cardWrap, cardUuid, cardName);
            }

            ProjectData proj = ClientProjectCache.getByOwner(cardUuid);
            if (proj != null && !proj.completed) {
                String projText = "🏗 " + truncateProjectName(proj.name, 18) + " — " + proj.progress + "%";
                if (proj.isHelpWanted()) projText += " ✦";
                FlowLayout projStrip = UIContainers.horizontalFlow(
                        Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.fixed(14));
                projStrip.surface(Surface.flat(0xCC0E1520));
                projStrip.padding(Insets.horizontal(6));
                projStrip.verticalAlignment(VerticalAlignment.CENTER);
                projStrip.child(UIComponents.label(Text.literal(projText).withColor(0xFF88BBFF)));
                cardWrap.child(projStrip);
            }

            grid.child(cardWrap);
        }

        if (players.isEmpty() && hub.searchQuery.isEmpty()) {
            grid.child(UIComponents.label(Text.literal("No players online.").formatted(Formatting.GRAY)));
        }

        boolean isSingleplayer = client.getServer() != null;

        if (!isSingleplayer && WavelengthConfig.get().showOfflineFriends) {
            List<FriendList.Friend> offlineFriends = FriendList.getAll().stream()
                    .filter(f -> !onlineUuids.contains(UUID.fromString(f.uuid())))
                    .filter(f -> matches(f.username()))
                    .toList();
            if (!offlineFriends.isEmpty()) {
                grid.child(sectionDividerLabel("◌  Offline Friends  (" + offlineFriends.size() + ")", 0xFF555577));
                for (FriendList.Friend f : offlineFriends) {
                    UUID fUuid = UUID.fromString(f.uuid());
                    grid.child(buildOfflineCard(fUuid, f.username(), PersistentPlayerCache.get(fUuid), true));
                }
            }
        }

        List<UUID> friendUuids = FriendList.getAll().stream().map(f -> UUID.fromString(f.uuid())).toList();
        UUID localUuid = me.getUuid();

        List<Map.Entry<UUID, PersistentPlayerCache.Entry>> offlinePlayers = isSingleplayer
                ? List.of()
                : PersistentPlayerCache.getAllExcept(localUuid).stream()
                .filter(e -> !onlineUuids.contains(e.getKey()))
                .filter(e -> !friendUuids.contains(e.getKey()))
                .filter(e -> !e.getValue().username.isBlank())
                .filter(e -> matches(e.getValue().username))
                .toList();

        if (!offlinePlayers.isEmpty()) {
            grid.child(sectionDividerLabel("◌  Offline Players  (" + offlinePlayers.size() + ")", 0xFF3D3D55));
            for (Map.Entry<UUID, PersistentPlayerCache.Entry> e : offlinePlayers) {
                grid.child(buildOfflineCard(e.getKey(), e.getValue().username, e.getValue(), false));
            }
        }

        if (players.isEmpty() && !hub.searchQuery.isEmpty()) {
            boolean hasAnyOffline =
                    (WavelengthConfig.get().showOfflineFriends && FriendList.getAll().stream()
                            .filter(f -> !onlineUuids.contains(UUID.fromString(f.uuid())))
                            .anyMatch(f -> matches(f.username())))
                            || offlinePlayers.stream().anyMatch(e -> matches(e.getValue().username));
            if (!hasAnyOffline) {
                grid.child(UIComponents.label(
                        Text.literal("No players match \"" + hub.searchQuery + "\".").formatted(Formatting.GRAY)));
            }
        }
    }

    private void buildLocalControls(FlowLayout cardWrap) {
        PlayerData data = PlayerData.get();
        hub.privacyControlsWrap = UIContainers.verticalFlow(
                Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.content());
        fillPrivacyControls(data);
        cardWrap.child(hub.privacyControlsWrap);

        boolean pvpOn = data.pvpMode;
        String pvpLabel = pvpOn ? "\u2694 PVP Mode: ON" : "\u2694 PVP Mode: OFF";
        int pvpNormal = pvpOn ? 0xFF3A0A0A : 0xFF0A1A0A;
        int pvpHover  = pvpOn ? 0xFF551515 : 0xFF153A15;
        int pvpColor  = pvpOn ? 0xFFFF6666 : 0xFF55FF88;
        cardWrap.child(styledBtn(PlayerCardComponent.CARD_W, 18,
                Text.literal(pvpLabel).withColor(pvpColor), pvpNormal, pvpHover,
                () -> {
                    playTick();
                    data.togglePvpMode();
                    WavelengthClient.pushLocalPlayerData();
                    hub.fillContent();
                }));
    }

    void fillPrivacyControls(PlayerData data) {
        if (hub.privacyControlsWrap == null) return;
        hub.privacyControlsWrap.clearChildren();
        PrivacyMode priv = data.privacy;

        String arrow = hub.privacyDropdownOpen ? "▲" : "▼";
        FlowLayout privSelector = styledBtn(
                PlayerCardComponent.CARD_W, 18,
                Text.literal(arrow + " " + privacyDot(priv) + "  " + priv.displayName).withColor(priv.dotColor),
                0xFF1A1A33, 0xFF252545,
                () -> {
                    playTick();
                    hub.privacyDropdownOpen = !hub.privacyDropdownOpen;
                    fillPrivacyControls(data);
                });
        hub.privacyControlsWrap.child(privSelector);

        if (hub.privacyDropdownOpen) {
            for (PrivacyMode mode : PrivacyMode.values()) {
                int optNormal = switch (mode) {
                    case PUBLIC  -> 0xFF0D280D;
                    case FRIENDS -> 0xFF28280D;
                    case GHOST   -> 0xFF280D0D;
                };
                int optHover = switch (mode) {
                    case PUBLIC  -> 0xFF174A17;
                    case FRIENDS -> 0xFF3A3A0D;
                    case GHOST   -> 0xFF3A0D0D;
                };
                String checkmark = (mode == priv) ? "● " : "  ";
                hub.privacyControlsWrap.child(styledBtn(
                        PlayerCardComponent.CARD_W, 18,
                        Text.literal(checkmark + privacyDot(mode) + "  " + mode.displayName)
                                .withColor(mode == priv ? mode.dotColor : 0xFFAAAAAA),
                        optNormal, optHover,
                        () -> {
                            data.privacy = mode;
                            data.save();
                            WavelengthClient.pushLocalPlayerData();
                            hub.privacyDropdownOpen = false;
                            fillPrivacyControls(data);
                        }));
            }
        }
    }

    private void buildFriendButtons(FlowLayout cardWrap, UUID cardUuid, String cardName) {
        boolean alreadyFriend   = FriendList.isFriend(cardUuid);
        boolean requestSent     = FriendRequests.hasSentTo(cardUuid);
        boolean requestReceived = FriendRequests.hasIncomingFrom(cardUuid);

        FlowLayout friendWrap = UIContainers.verticalFlow(
                Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.content());

        if (alreadyFriend) {
            friendWrap.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                    Text.literal("★ Friends").withColor(0xFFFFD700), 0xFF1A2A1A, 0xFF254025,
                    () -> { hub.activeTab = HubScreen.Tab.FRIENDS; hub.fillTabBar(); hub.fillContent(); }));
        } else if (requestReceived) {
            friendWrap.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                    Text.literal("✓ Accept Request").withColor(0xFF88FF44), 0xFF1A3A00, 0xFF255500,
                    () -> {
                        playSuccess();
                        cleanFriendState(cardUuid);
                        if (ServerModDetector.isPresent()) {
                            try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(cardUuid, "ACCEPT")); }
                            catch (Exception ignored) {}
                        } else { FriendList.add(cardUuid, cardName); }
                        hub.fillTabBar(); hub.fillContent();
                    }));
        } else if (requestSent) {
            friendWrap.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                    Text.literal("⏳ Pending — Cancel").withColor(0xFF888888), 0xFF222233, 0xFF2E2E44,
                    () -> { playNegative(); sendFriendAction(cardUuid, cardName, "CANCEL"); }));
        } else if (ServerModDetector.isPresent()) {
            friendWrap.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                    Text.literal("☆ Send Request").withColor(0xFFAAAAFF), 0xFF1A1A3A, 0xFF282855,
                    () -> sendFriendAction(cardUuid, cardName, "REQUEST")));
        }
        cardWrap.child(friendWrap);
    }

    FlowLayout buildOfflineCard(UUID fUuid, String fName,
                                PersistentPlayerCache.Entry cached, boolean isFriend) {
        FlowLayout card = UIContainers.verticalFlow(
                Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.content());
        card.surface(Surface.flat(0xBB0D0D1A));
        card.margins(Insets.of(4));
        card.gap(0);

        FlowLayout nameRow = UIContainers.horizontalFlow(
                Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.fixed(20));
        nameRow.surface(Surface.flat(0xCC141422));
        nameRow.padding(Insets.horizontal(8));
        nameRow.gap(4);
        nameRow.verticalAlignment(VerticalAlignment.CENTER);

        String displayName = (cached != null && !cached.username.isBlank()) ? cached.username : fName;
        FlowLayout nameInner = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        nameInner.gap(4);
        nameInner.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent nameLbl = UIComponents.label(
                Text.literal("\u25CB  " + displayName).withColor(isFriend ? 0xFF8888AA : 0xFF666688));
        nameLbl.shadow(true);
        nameInner.child(nameLbl);
        if (isFriend) nameInner.child(UIComponents.label(Text.literal("★").withColor(0xFF887722)));
        nameRow.child(nameInner);
        nameRow.child(UIComponents.label(Text.literal("Offline").withColor(0xFF444466)));
        card.child(nameRow);

        String rankLine = "";
        if (cached != null) rankLine = cached.rankLine();
        if (rankLine.isEmpty()) rankLine = RemotePlayerCache.rankLine(fUuid);
        if (!rankLine.isEmpty()) {
            FlowLayout rankRow = UIContainers.horizontalFlow(
                    Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.fixed(14));
            rankRow.surface(Surface.flat(0xAA0E0E20));
            rankRow.padding(Insets.horizontal(8));
            rankRow.verticalAlignment(VerticalAlignment.CENTER);
            rankRow.child(UIComponents.label(Text.literal(rankLine).withColor(0xFF887744)));
            card.child(rankRow);
        }

        Achievement topBadge = computeTopBadgeFromCache(cached);
        if (cached != null && (cached.playtimeTicks > 0 || topBadge != Achievement.NONE)) {
            FlowLayout infoRow = UIContainers.horizontalFlow(
                    Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.fixed(14));
            infoRow.surface(Surface.flat(0xAA0A0A1A));
            infoRow.padding(Insets.horizontal(8));
            infoRow.gap(4);
            infoRow.verticalAlignment(VerticalAlignment.CENTER);
            if (cached.playtimeTicks > 0) {
                LabelComponent ptLbl = UIComponents.label(
                        Text.literal("\u23F1 " + formatDuration(cached.playtimeTicks / 20)).withColor(0xFF888899));
                infoRow.child(ptLbl);
            }

            infoRow.child(UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(1)));
            if (topBadge != Achievement.NONE) {
                infoRow.child(UIComponents.label(
                        Text.literal(PlayerCardComponent.badgeIcon(topBadge) + " " + topBadge.displayName)
                                .withColor(topBadge.color)));
            }
            card.child(infoRow);
        }

        if (!isFriend) {
            if (FriendRequests.hasIncomingFrom(fUuid)) {
                card.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                        Text.literal("✓ Accept Request").withColor(0xFF88FF44), 0xFF1A3A00, 0xFF255500,
                        () -> {
                            playSuccess();
                            cleanFriendState(fUuid);
                            if (ServerModDetector.isPresent()) {
                                try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(fUuid, "ACCEPT")); }
                                catch (Exception ignored) {}
                            } else { FriendList.add(fUuid, fName); }
                            hub.fillTabBar(); hub.fillContent();
                        }));
            } else if (FriendRequests.hasSentTo(fUuid)) {
                card.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                        Text.literal("⏳ Pending — Cancel").withColor(0xFF888888), 0xFF222233, 0xFF2E2E44,
                        () -> { playNegative(); sendFriendAction(fUuid, fName, "CANCEL"); }));
            } else if (ServerModDetector.isPresent()) {
                card.child(styledBtn(PlayerCardComponent.CARD_W, 16,
                        Text.literal("☆ Send Request").withColor(0xFFAAAAFF), 0xFF1A1A3A, 0xFF282855,
                        () -> sendFriendAction(fUuid, fName, "REQUEST")));
            }
        }

        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.fixed(PlayerCardComponent.CARD_W), Sizing.content());
        btnRow.gap(0);
        int statsW = isFriend ? PlayerCardComponent.CARD_W / 2 : PlayerCardComponent.CARD_W;
        if (cached != null) {
            btnRow.child(styledBtn(statsW, 16,
                    Text.literal("\uD83D\uDCCA View Stats").withColor(0xFFAAAAFF), 0xFF1A1A3A, 0xFF282855,
                    () -> {
                        playClick();
                        hub.selectedStatsUuid = fUuid;
                        hub.activeTab = HubScreen.Tab.STATS;
                        hub.fillTabBar();
                        hub.fillContent();
                    }));
        }
        if (isFriend) {
            int removW = cached != null ? PlayerCardComponent.CARD_W - statsW : PlayerCardComponent.CARD_W;
            btnRow.child(styledBtn(removW, 16,
                    Text.literal("\u2717 Remove").withColor(0xFF774444), 0xFF1A0F0F, 0xFF2A1515,
                    () -> {
                        playNegative();
                        FriendList.remove(fUuid);
                        cleanFriendState(fUuid);
                        if (ServerModDetector.isPresent()) {
                            try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(fUuid, "UNFRIEND")); }
                            catch (Exception ignored) {}
                        }
                        hub.fillTabBar(); hub.fillContent();
                    }));
        }
        if (!btnRow.children().isEmpty()) card.child(btnRow);
        return card;
    }

    void sendFriendAction(UUID targetUuid, String targetName, String action) {
        switch (action) {
            case "REQUEST" -> {
                if (FriendList.isFriend(targetUuid)) return;
                if (FriendRequests.hasSentTo(targetUuid)) return;
                FriendRequests.removeIncoming(targetUuid);
            }
            case "UNFRIEND" -> {
                FriendRequests.removeIncoming(targetUuid);
                FriendRequests.removeOutgoing(targetUuid);
            }
        }
        if (ServerModDetector.isPresent()) {
            try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(targetUuid, action)); }
            catch (Exception ignored) {}
            switch (action) {
                case "REQUEST"  -> FriendRequests.addOutgoing(targetUuid, targetName);
                case "CANCEL"   -> FriendRequests.removeOutgoing(targetUuid);
                case "UNFRIEND" -> FriendList.remove(targetUuid);
            }
        } else {
            switch (action) {
                case "REQUEST"  -> FriendList.add(targetUuid, targetName);
                case "UNFRIEND" -> FriendList.remove(targetUuid);
            }
        }
        MinecraftClient.getInstance().execute(() -> { hub.fillTabBar(); hub.fillContent(); });
    }

    static void cleanFriendState(UUID uuid) {
        FriendRequests.removeIncoming(uuid);
        FriendRequests.removeOutgoing(uuid);
    }

    private boolean matches(String name) {
        if (hub.searchQuery == null || hub.searchQuery.isEmpty()) return true;
        return name != null && name.toLowerCase().contains(hub.searchQuery);
    }

    private static String truncateProjectName(String name, int maxLen) {
        if (name == null) return "";
        if (name.length() <= maxLen) return name;
        return name.substring(0, maxLen - 1) + "…";
    }
}