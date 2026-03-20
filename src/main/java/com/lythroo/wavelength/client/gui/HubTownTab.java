package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.WavelengthClient;
import com.lythroo.wavelength.common.data.*;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubTownTab {

    private final HubScreen hub;

    HubTownTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        if (!hub.pendingInitialized) {
            PlayerData d = PlayerData.get();
            hub.pendingTownName    = d.townName != null ? d.townName : "";
            hub.pendingRank        = d.rank     != null ? d.rank     : Rank.NONE;
            hub.pendingInitialized = true;
        }

        PlayerData data = PlayerData.get();
        int innerW = Math.min(hub.panelW, hub.width - 40);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(16));
        panel.gap(8);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        panel.child(sectionHeader("EDIT YOUR IDENTITY"));
        panel.child(divider(hub.panelW));

        panel.child(UIComponents.label(Text.literal("Town Name:").withColor(COL_HEADER_TXT)));
        var townField = UIComponents.textBox(Sizing.fixed(Math.min(220, innerW - 20)));
        townField.setMaxLength(40);
        townField.setText(hub.pendingTownName);
        townField.onChanged().subscribe(s -> { hub.pendingTownName = s; refreshPreview(); });
        panel.child(townField);
        panel.child(UIComponents.label(Text.literal("e.g. Riverside, Oakwood, New Hope").withColor(0xFF555577)));
        panel.child(spacer(6));

        panel.child(UIComponents.label(Text.literal("Your Rank:").withColor(COL_HEADER_TXT)));
        FlowLayout rankGrid = UIContainers.ltrTextFlow(Sizing.fixed(innerW), Sizing.content());
        rankGrid.gap(4);
        hub.rankGridRef = rankGrid;

        java.util.List<Rank> rankOptions = new java.util.ArrayList<>(java.util.Arrays.asList(Rank.selectable()));
        net.minecraft.client.MinecraftClient _mc = net.minecraft.client.MinecraftClient.getInstance();
        boolean isDevAccount = _mc.player != null
                && java.util.UUID.fromString("6b5c34ea-ad7e-4717-931e-b6861a5efc8c").equals(_mc.player.getUuid());
        if (isDevAccount) rankOptions.add(0, Rank.DEVELOPER);

        for (Rank r : rankOptions) {
            boolean sel   = (r == hub.pendingRank);
            String  lbl_  = (r == Rank.NONE ? "None" : r.displayName);
            int normalBg = sel ? 0xFF3A3A7A : 0xFF1E1E3A;
            int hoverBg  = sel ? 0xFF3A3A7A : 0xFF2A2A55;
            int txtColor = sel ? 0xFFDDDDFF : 0xFF888899;
            Rank rRef = r;
            rankGrid.child(styledBtn(74, 18,
                    Text.literal(sel ? "▶ " + lbl_ : lbl_).withColor(txtColor),
                    normalBg, hoverBg,
                    () -> { hub.pendingRank = rRef; refreshRankGrid(); refreshPreview(); }));
        }
        panel.child(rankGrid);
        panel.child(spacer(6));

        panel.child(UIComponents.label(Text.literal("Preview:").withColor(COL_GRAY)));
        String preview = hub.pendingRank.format(hub.pendingTownName);
        hub.previewLabel = UIComponents.label(preview.isEmpty()
                ? Text.literal("(no rank set)").formatted(Formatting.GRAY)
                : Text.literal(preview).withColor(COL_GOLD));
        panel.child(hub.previewLabel);
        panel.child(spacer(10));

        panel.child(divider(hub.panelW));
        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        btnRow.gap(8);

        btnRow.child(styledBtn(110, 22, Text.literal("✔  Save").withColor(0xFF55FF55), 0xFF153A15, 0xFF1F5A1F,
                () -> {
                    playSuccess();
                    data.townName = hub.pendingTownName;
                    data.rank     = hub.pendingRank;
                    data.save();
                    WavelengthClient.pushLocalPlayerData();
                    hub.fillContent();
                }));

        btnRow.child(styledBtn(110, 22, Text.literal("✗  Clear").withColor(0xFFFF5555), 0xFF3A1515, 0xFF5A1F1F,
                () -> {
                    playNegative();
                    hub.pendingTownName = ""; hub.pendingRank = Rank.NONE; hub.pendingInitialized = true;
                    data.townName = ""; data.rank = Rank.NONE;
                    data.save();
                    WavelengthClient.pushLocalPlayerData();
                    hub.fillContent();
                }));

        panel.child(btnRow);
        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    void refreshRankGrid() {
        if (hub.rankGridRef == null) return;
        hub.rankGridRef.clearChildren();
        java.util.List<Rank> rankOptions = new java.util.ArrayList<>(java.util.Arrays.asList(Rank.selectable()));
        net.minecraft.client.MinecraftClient _mc = net.minecraft.client.MinecraftClient.getInstance();
        boolean isDevAccount = _mc.player != null
                && java.util.UUID.fromString("6b5c34ea-ad7e-4717-931e-b6861a5efc8c").equals(_mc.player.getUuid());
        if (isDevAccount) rankOptions.add(0, Rank.DEVELOPER);
        for (Rank r : rankOptions) {
            boolean sel  = (r == hub.pendingRank);
            String  lbl_ = (r == Rank.NONE ? "None" : r.displayName);
            int normalBg = sel ? 0xFF3A3A7A : 0xFF1E1E3A;
            int hoverBg  = sel ? 0xFF3A3A7A : 0xFF2A2A55;
            int txtColor = sel ? 0xFFDDDDFF : 0xFF888899;
            Rank rRef = r;
            hub.rankGridRef.child(styledBtn(74, 18,
                    Text.literal(sel ? "▶ " + lbl_ : lbl_).withColor(txtColor),
                    normalBg, hoverBg,
                    () -> { hub.pendingRank = rRef; refreshRankGrid(); refreshPreview(); }));
        }
    }

    void refreshPreview() {
        if (hub.previewLabel == null) return;
        String p = hub.pendingRank.format(hub.pendingTownName);
        hub.previewLabel.text(p.isEmpty()
                ? Text.literal("(no rank set)").formatted(Formatting.GRAY)
                : Text.literal(p).withColor(COL_GOLD));
    }
}