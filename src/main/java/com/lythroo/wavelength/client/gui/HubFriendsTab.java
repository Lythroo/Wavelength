package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.network.ServerModDetector;
import com.lythroo.wavelength.common.network.SyncPacket;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;
import static com.lythroo.wavelength.client.gui.HubPlayersTab.cleanFriendState;

final class HubFriendsTab {

    private final HubScreen hub;

    HubFriendsTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        int innerW = Math.min(hub.panelW, hub.width - 40);
        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(16));
        panel.gap(8);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        if (!ServerModDetector.isPresent()) {
            FlowLayout notice = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
            notice.surface(Surface.flat(0xFF2A1A00));
            notice.padding(Insets.of(6));
            notice.gap(4);
            notice.child(UIComponents.label(
                    Text.literal("⚠ Friend requests require the Wavelength server mod.\n" +
                            "  When absent, the Add button adds directly (no confirmation).").withColor(0xFFFFAA44)));
            panel.child(notice);
            panel.child(spacer(6));
        }

        List<FriendRequests.PendingRequest> incomingList = new ArrayList<>(FriendRequests.getIncoming());
        panel.child(sectionHeader("📩 INCOMING REQUESTS  (" + incomingList.size() + ")"));
        panel.child(divider(hub.panelW));

        if (incomingList.isEmpty()) {
            panel.child(UIComponents.label(Text.literal("No pending requests.").withColor(0xFF555577)));
        } else {
            for (FriendRequests.PendingRequest req : incomingList) {
                UUID   reqUuid = UUID.fromString(req.uuid());
                String reqName = req.username();
                FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(24));
                row.surface(Surface.flat(0xFF1A1A2E));
                row.padding(Insets.horizontal(6));
                row.gap(6);
                row.verticalAlignment(VerticalAlignment.CENTER);
                LabelComponent nameLbl = UIComponents.label(Text.literal("☆ " + reqName).withColor(0xFFCCCCFF));
                nameLbl.sizing(Sizing.fixed(Math.max(40, innerW - 12 - 64 - 6 - 66 - 6)), Sizing.content());
                row.child(nameLbl);
                row.child(styledBtn(64, 18, Text.literal("✓ Accept").withColor(0xFF55FF55), 0xFF153A15, 0xFF1F5A1F,
                        () -> {
                            playSuccess();
                            cleanFriendState(reqUuid);
                            if (ServerModDetector.isPresent()) {
                                try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(reqUuid, "ACCEPT")); }
                                catch (Exception ignored) {}
                            } else { FriendList.add(reqUuid, reqName); }
                            hub.fillTabBar(); hub.fillContent();
                        }));
                row.child(styledBtn(66, 18, Text.literal("✗ Decline").withColor(0xFFFF5555), 0xFF3A1515, 0xFF5A1F1F,
                        () -> {
                            playNegative();
                            FriendRequests.removeIncoming(reqUuid);
                            if (ServerModDetector.isPresent()) {
                                try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(reqUuid, "DECLINE")); }
                                catch (Exception ignored) {}
                            }
                            hub.fillTabBar(); hub.fillContent();
                        }));
                panel.child(row);
            }
        }
        panel.child(spacer(10));

        List<FriendRequests.PendingRequest> outgoingList = new ArrayList<>(FriendRequests.getOutgoing());
        panel.child(sectionHeader("📤 SENT REQUESTS  (" + outgoingList.size() + ")"));
        panel.child(divider(hub.panelW));
        if (outgoingList.isEmpty()) {
            panel.child(UIComponents.label(Text.literal("No outgoing requests.").withColor(0xFF555577)));
        } else {
            for (FriendRequests.PendingRequest req : outgoingList) {
                UUID   reqUuid = UUID.fromString(req.uuid());
                FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(24));
                row.surface(Surface.flat(0xFF1A1A2E));
                row.padding(Insets.horizontal(6));
                row.gap(6);
                row.verticalAlignment(VerticalAlignment.CENTER);
                LabelComponent nameLbl = UIComponents.label(Text.literal("⏳ " + req.username()).withColor(0xFF888888));
                nameLbl.sizing(Sizing.fixed(Math.max(40, innerW - 12 - 64 - 6)), Sizing.content());
                row.child(nameLbl);
                row.child(styledBtn(64, 18, Text.literal("✗ Cancel").withColor(0xFFAAAA55), 0xFF2A2A15, 0xFF3A3A1F,
                        () -> {
                            playNegative();
                            FriendRequests.removeOutgoing(reqUuid);
                            if (ServerModDetector.isPresent()) {
                                try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(reqUuid, "CANCEL")); }
                                catch (Exception ignored) {}
                            }
                            hub.fillTabBar(); hub.fillContent();
                        }));
                panel.child(row);
            }
        }
        panel.child(spacer(10));

        List<FriendList.Friend> allFriends = new ArrayList<>(FriendList.getAll());
        MinecraftClient client = MinecraftClient.getInstance();
        panel.child(sectionHeader("★ FRIENDS  (" + allFriends.size() + ")"));
        panel.child(divider(hub.panelW));
        if (ServerModDetector.isPresent()) {
            panel.child(UIComponents.label(
                    Text.literal("Send requests via the Players tab  →  ☆ Send Request").withColor(COL_GRAY)));
            panel.child(spacer(4));
        }

        if (allFriends.isEmpty()) {
            panel.child(UIComponents.label(Text.literal("No friends yet.").withColor(0xFF555577)));
        } else {
            for (FriendList.Friend friend : allFriends) {
                UUID friendUuid = UUID.fromString(friend.uuid());
                boolean online = false;
                if (client.world != null) {
                    for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                        if (p.getUuid().equals(friendUuid) || p.getName().getString().equals(friend.username())) {
                            online = true; break;
                        }
                    }
                }
                FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(24));
                row.surface(Surface.flat(0x11FFFFFF));
                row.padding(Insets.horizontal(6));
                row.gap(6);
                row.verticalAlignment(VerticalAlignment.CENTER);
                LabelComponent nameLbl = UIComponents.label(
                        Text.literal("★ " + friend.username() + (online ? " [online]" : ""))
                                .withColor(online ? 0xFF88FF88 : 0xFF888888));
                nameLbl.sizing(Sizing.fixed(Math.max(40, innerW - 12 - 66 - 6)), Sizing.content());
                row.child(nameLbl);
                row.child(styledBtn(66, 18, Text.literal("✗ Remove").withColor(0xFFFF5555), 0xFF3A1515, 0xFF5A1F1F,
                        () -> {
                            playNegative();
                            FriendList.remove(friendUuid);
                            cleanFriendState(friendUuid);
                            if (ServerModDetector.isPresent()) {
                                try { ClientPlayNetworking.send(new SyncPacket.FriendActionC2SPayload(friendUuid, "UNFRIEND")); }
                                catch (Exception ignored) {}
                            }
                            hub.fillTabBar(); hub.fillContent();
                        }));
                panel.child(row);
            }
        }
        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }
}