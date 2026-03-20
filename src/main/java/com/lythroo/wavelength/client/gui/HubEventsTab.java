package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.ClientEventCache;
import com.lythroo.wavelength.common.data.ClientDimensionGateCache;
import com.lythroo.wavelength.common.data.EventData;
import com.lythroo.wavelength.common.network.SyncPacket;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubEventsTab {

    private static final int COL_ACCENT    = 0xFF55DDFF;
    private static final int COL_SCHED     = 0xFFFFAA44;
    private static final int COL_FIELD_LBL = 0xFF8899AA;

    private final HubScreen hub;

    private final java.util.List<Runnable> countdownUpdaters = new java.util.ArrayList<>();

    HubEventsTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        if (hub.eventFormOpen) return buildFormMode();
        return buildViewMode();
    }

    private UIComponent buildViewMode() {
        int innerW = Math.min(hub.panelW - 32, hub.width - 50);
        countdownUpdaters.clear();

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(12));
        panel.gap(6);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        UUID localUuid = localUuid();
        List<EventData> myEvents  = localUuid != null ? ClientEventCache.getByOwner(localUuid) : List.of();
        List<EventData> allEvents = ClientEventCache.getActive();
        boolean canCreate = localUuid != null && myEvents.size() < EventData.MAX_EVENTS_PER_PLAYER;

        MinecraftClient _mc = MinecraftClient.getInstance();
        PermissionPredicate _perms = _mc.player != null ? _mc.player.getPermissions() : PermissionPredicate.NONE;
        boolean isOp = _perms == LeveledPermissionPredicate.GAMEMASTERS
                || _perms == LeveledPermissionPredicate.ADMINS
                || _perms == LeveledPermissionPredicate.OWNERS;

        if (isOp) {
            panel.child(buildOpControlsPanel(innerW));
            panel.child(spacer(12));
        }

        int btnW = canCreate ? 96 : 0;
        FlowLayout headerRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(22));
        headerRow.verticalAlignment(VerticalAlignment.CENTER);
        headerRow.gap(8);
        LabelComponent hdr = UIComponents.label(Text.literal("📅  EVENTS").withColor(0xFFBBAAFF));
        hdr.shadow(true);
        hdr.sizing(Sizing.fixed(innerW - btnW - (btnW > 0 ? 8 : 0)), Sizing.content());
        headerRow.child(hdr);
        if (canCreate) {
            headerRow.child(styledBtn(btnW, 18,
                    Text.literal("＋  New Event").withColor(0xFF88FF88),
                    0xFF0A200A, 0xFF143514, () -> openForm(null)));
        } else if (localUuid != null) {
            headerRow.child(UIComponents.label(Text.literal("(limit reached)").withColor(0xFF445566)));
        }
        panel.child(headerRow);
        panel.child(divider(hub.panelW));

        List<EventData> upcoming = allEvents.stream().filter(EventData::isScheduled).toList();
        List<EventData> live     = allEvents.stream().filter(e -> !e.isScheduled()).toList();

        if (!upcoming.isEmpty()) {
            panel.child(spacer(2));
            panel.child(sectionHeader("⏰  UPCOMING  (" + upcoming.size() + ")"));
            panel.child(divider(hub.panelW));
            for (EventData e : upcoming) panel.child(buildNewsCard(e, innerW, localUuid, true));
        }

        panel.child(spacer(4));
        panel.child(sectionHeader("🌐  LIVE NOW  (" + live.size() + ")"));
        panel.child(divider(hub.panelW));

        if (live.isEmpty()) {
            panel.child(spacer(6));
            panel.child(UIComponents.label(
                    Text.literal("No events running right now.").withColor(COL_GRAY)));
        } else {
            for (EventData e : live) panel.child(buildNewsCard(e, innerW, localUuid, false));
        }

        panel.child(spacer(16));

        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private FlowLayout buildNewsCard(EventData e, int innerW, UUID localUuid, boolean upcoming) {
        boolean isOwn       = localUuid != null && localUuid.toString().equals(e.ownerUuid);
        int     accentColor = upcoming ? COL_SCHED : COL_ACCENT;
        int     cardBg      = isOwn ? 0xCC071018 : 0xCC0A0A18;

        FlowLayout outer = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        outer.surface(Surface.flat(cardBg));
        outer.margins(Insets.vertical(4));
        outer.gap(0);

        FlowLayout stripe = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(4));
        stripe.surface(Surface.flat(accentColor));
        outer.child(stripe);

        FlowLayout body = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        body.padding(Insets.of(10, 12, 10, 12));
        body.gap(5);
        outer.child(body);

        int cw = innerW - 24;

        int badgeW = (isOwn ? 36 : 0) + (upcoming ? 44 : 0) + ((isOwn && upcoming) ? 6 : (isOwn || upcoming) ? 6 : 0);
        FlowLayout nameRow = UIContainers.horizontalFlow(Sizing.fixed(cw), Sizing.content());
        nameRow.gap(6);
        nameRow.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent nameLbl = UIComponents.label(Text.literal(e.name).withColor(0xFFEEF4FF));
        nameLbl.shadow(true);
        nameLbl.sizing(Sizing.fixed(Math.max(20, cw - badgeW - 6)), Sizing.content());
        nameRow.child(nameLbl);
        if (upcoming) nameRow.child(pillBadge("SOON", 0xFF1A1000, COL_SCHED));
        if (isOwn)    nameRow.child(pillBadge("YOU",  0xFF061018, accentColor));
        body.child(nameRow);

        body.child(UIComponents.label(
                Text.literal("by " + e.ownerName + "  ·  " + e.timeAgo()).withColor(0xFF44566A)));

        FlowLayout innerDiv = UIContainers.horizontalFlow(Sizing.fixed(cw), Sizing.fixed(1));
        innerDiv.surface(Surface.flat(0x22FFFFFF));
        innerDiv.margins(Insets.vertical(2));
        body.child(innerDiv);

        if (e.description != null && !e.description.isBlank())
            body.child(UIComponents.label(Text.literal(e.description).withColor(0xFF8FA8C0)));

        FlowLayout chipRow = UIContainers.horizontalFlow(Sizing.fixed(cw), Sizing.content());
        chipRow.gap(5);
        chipRow.verticalAlignment(VerticalAlignment.CENTER);
        boolean anyChip = false;

        if (upcoming && e.scheduledStartMs > 0) {
            long diff0 = e.scheduledStartMs - System.currentTimeMillis();
            if (diff0 > 0) {
                LabelComponent startLbl = UIComponents.label(
                        Text.literal("⏰ starts in " + fmtCountdown(diff0)).withColor(COL_SCHED));
                FlowLayout startChip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(14));
                startChip.surface(Surface.flat(0xFF1A1000));
                startChip.padding(Insets.horizontal(6));
                startChip.verticalAlignment(VerticalAlignment.CENTER);
                startChip.child(startLbl);
                chipRow.child(startChip);
                anyChip = true;
                final long startMs = e.scheduledStartMs;
                countdownUpdaters.add(() -> {
                    long d = startMs - System.currentTimeMillis();
                    if (d > 0) startLbl.text(Text.literal("⏰ starts in " + fmtCountdown(d)).withColor(COL_SCHED));
                    else        startLbl.text(Text.literal("⏰ starting…").withColor(COL_SCHED));
                });
            }
        }
        if (e.endMs > 0 && !upcoming) {
            long diff0 = e.endMs - System.currentTimeMillis();
            if (diff0 > 0) {
                LabelComponent endLbl = UIComponents.label(
                        Text.literal("⏱ ends in " + fmtCountdown(diff0)).withColor(0xFF55AACC));
                FlowLayout endChip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(14));
                endChip.surface(Surface.flat(0xFF091616));
                endChip.padding(Insets.horizontal(6));
                endChip.verticalAlignment(VerticalAlignment.CENTER);
                endChip.child(endLbl);
                chipRow.child(endChip);
                anyChip = true;
                final long endMs = e.endMs;
                countdownUpdaters.add(() -> {
                    long d = endMs - System.currentTimeMillis();
                    if (d > 0) endLbl.text(Text.literal("⏱ ends in " + fmtCountdown(d)).withColor(0xFF55AACC));
                    else        endLbl.text(Text.literal("✓ ended").withColor(0xFF445566));
                });
            } else {
                chipRow.child(infoChip("✓ ended", 0xFF111114, 0xFF445566));
                anyChip = true;
            }
        }
        if (e.hasLocation) {
            chipRow.child(infoChip(String.format("📍 %d, %d, %d", e.locX, e.locY, e.locZ), 0xFF091209, 0xFF55AA77));
            anyChip = true;
        }
        if (anyChip) body.child(chipRow);

        MinecraftClient _mc = MinecraftClient.getInstance();
        PermissionPredicate _perms = _mc.player != null ? _mc.player.getPermissions() : PermissionPredicate.NONE;
        boolean isOp = _perms == LeveledPermissionPredicate.GAMEMASTERS
                || _perms == LeveledPermissionPredicate.ADMINS
                || _perms == LeveledPermissionPredicate.OWNERS;

        if (isOwn || isOp) {
            FlowLayout btnDiv = UIContainers.horizontalFlow(Sizing.fixed(cw), Sizing.fixed(1));
            btnDiv.surface(Surface.flat(0x22FFFFFF));
            btnDiv.margins(Insets.vertical(3));
            body.child(btnDiv);
            FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.fixed(cw), Sizing.content());
            btnRow.gap(8);
            if (isOwn) {
                btnRow.child(styledBtn(60, 18, Text.literal("✎  Edit").withColor(0xFF88AACC),
                        0xFF0A1520, 0xFF0F2232, () -> openForm(e)));
                btnRow.child(styledBtn(65, 18, Text.literal("✗  End").withColor(0xFFFF6666),
                        0xFF200A0A, 0xFF321010, () -> {

                            MinecraftClient _snd = MinecraftClient.getInstance();
                            if (_snd.player != null) {
                                _snd.player.playSound(net.minecraft.sound.SoundEvents.UI_TOAST_OUT, 0.8f, 0.85f);
                            }
                            try { ClientPlayNetworking.send(new SyncPacket.EventActionC2SPayload(e.id, "END")); }
                            catch (Exception ex) {  }
                            hub.fillContent();
                        }));
            }
            if (isOp) {
                btnRow.child(styledBtn(70, 18, Text.literal("🗑  Delete").withColor(0xFFFF4444),
                        0xFF2A0808, 0xFF3A1010, () -> {
                            MinecraftClient _snd = MinecraftClient.getInstance();
                            if (_snd.player != null) {
                                _snd.player.playSound(net.minecraft.sound.SoundEvents.UI_TOAST_OUT, 0.9f, 0.6f);
                            }
                            try { ClientPlayNetworking.send(new SyncPacket.EventActionC2SPayload(e.id, "DELETE")); }
                            catch (Exception ex) {  }
                            hub.fillContent();
                        }));
            }
            body.child(btnRow);
        }

        int hoverBg = (cardBg & 0x00FFFFFF) | 0xDD000000;
        outer.mouseEnter().subscribe(() -> outer.surface(Surface.flat(hoverBg)));
        outer.mouseLeave().subscribe(() -> outer.surface(Surface.flat(cardBg)));

        return outer;
    }

    private UIComponent buildFormMode() {

        int innerW = Math.min(hub.panelW, hub.width - 40);
        int fieldW = Math.min(220, innerW - 20);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(16));
        panel.gap(8);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        boolean isEdit = !hub.eventFormId.isEmpty();

        panel.child(sectionHeader(isEdit ? "✎  EDIT EVENT" : "✚  NEW EVENT"));
        panel.child(divider(hub.panelW));

        FlowLayout backRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        backRow.child(styledBtn(60, 16, Text.literal("← Back").withColor(0xFFCCCCFF),
                0xFF1E1E3A, 0xFF2E2E55, () -> { cancelForm(); hub.fillContent(); }));
        panel.child(backRow);

        panel.child(UIComponents.label(Text.literal("Event Name:").withColor(COL_HEADER_TXT)));
        var nameField = UIComponents.textBox(Sizing.fixed(fieldW));
        nameField.setText(hub.eventFormName);
        nameField.setMaxLength(40);
        nameField.onChanged().subscribe(v -> hub.eventFormName = v);
        panel.child(nameField);

        panel.child(UIComponents.label(Text.literal("Description (optional):").withColor(COL_HEADER_TXT)));
        var descField = UIComponents.textBox(Sizing.fixed(fieldW));
        descField.setText(hub.eventFormDesc);
        descField.setMaxLength(80);
        descField.onChanged().subscribe(v -> hub.eventFormDesc = v);
        panel.child(descField);

        boolean instant = hub.eventFormInstant;
        int instNormal = instant ? 0xFF1A1A00 : 0xFF1E1E3A;
        int instHover  = instant ? 0xFF2A2A00 : 0xFF2E2E55;
        panel.child(styledBtn(fieldW, 18,
                Text.literal(instant ? "⚡ One-time trigger: ON" : "⚡ One-time trigger: OFF")
                        .withColor(instant ? 0xFFFFFF55 : COL_GRAY),
                instNormal, instHover,
                () -> { hub.eventFormInstant = !hub.eventFormInstant; hub.fillContent(); }));
        if (instant) {
            panel.child(UIComponents.label(
                    Text.literal("→ Triggers once and disappears — great for toggling dimensions")
                            .withColor(0xFF888855)));
        }

        long schedDurMs = parseDurationMs(hub.eventFormSchedStart);
        panel.child(UIComponents.label(Text.literal("Start Delay  (30m / 2h / 1d, blank = now):").withColor(COL_HEADER_TXT)));
        var schedField = UIComponents.textBox(Sizing.fixed(fieldW));
        schedField.setText(hub.eventFormSchedStart);
        schedField.setMaxLength(8);
        schedField.onChanged().subscribe(v -> hub.eventFormSchedStart = v);
        panel.child(schedField);
        if (!hub.eventFormSchedStart.isBlank()) {
            boolean valid = schedDurMs > 0;
            panel.child(UIComponents.label(
                    Text.literal(valid ? "→ starts in " + fmtDur(schedDurMs) : "✗ invalid format")
                            .withColor(valid ? 0xFF88FF88 : 0xFFFF5555)));
        } else {
            panel.child(UIComponents.label(Text.literal(instant ? "→ triggers when this time runs out" : "→ starts immediately").withColor(0xFF555577)));
        }

        if (!instant) {

            long endDurMs = parseDurationMs(hub.eventFormEndTime);
            panel.child(UIComponents.label(Text.literal("Duration  (1h / 90m / 2d, blank = no end):").withColor(COL_HEADER_TXT)));
            var endField = UIComponents.textBox(Sizing.fixed(fieldW));
            endField.setText(hub.eventFormEndTime);
            endField.setMaxLength(8);
            endField.onChanged().subscribe(v -> hub.eventFormEndTime = v);
            panel.child(endField);
            if (!hub.eventFormEndTime.isBlank()) {
                boolean valid = endDurMs > 0;
                String preview = valid
                        ? "→ lasts " + fmtDur(endDurMs) + (schedDurMs > 0 ? " after start" : "")
                        : "✗ invalid format";
                panel.child(UIComponents.label(Text.literal(preview)
                        .withColor(valid ? 0xFF88FF88 : 0xFFFF5555)));
            } else {
                panel.child(UIComponents.label(Text.literal("→ no scheduled end").withColor(0xFF555577)));
            }
        }

        panel.child(UIComponents.label(Text.literal("Location (optional):").withColor(COL_HEADER_TXT)));
        boolean locOn = hub.eventFormHasLoc;
        panel.child(styledBtn(104, 18,
                Text.literal(locOn ? "☑ Location On" : "☐ Location Off")
                        .withColor(locOn ? 0xFF55FF88 : COL_GRAY),
                locOn ? 0xFF153A15 : 0xFF1E1E3A,
                locOn ? 0xFF1F5A1F : 0xFF2E2E55,
                () -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (!hub.eventFormHasLoc && mc.player != null) {
                        hub.eventFormHasLoc = true;
                        hub.eventFormLocX   = (long) mc.player.getX();
                        hub.eventFormLocY   = (long) mc.player.getY();
                        hub.eventFormLocZ   = (long) mc.player.getZ();
                    } else {
                        hub.eventFormHasLoc = false;
                    }
                    hub.fillContent();
                }));
        if (hub.eventFormHasLoc) {
            panel.child(UIComponents.label(
                    Text.literal(String.format("📍 %d, %d, %d",
                                    hub.eventFormLocX, hub.eventFormLocY, hub.eventFormLocZ))
                            .withColor(0xFF55AA77)));
        }

        panel.child(divider(hub.panelW));
        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        btnRow.gap(8);
        btnRow.child(styledBtn(isEdit ? 80 : 110, 22,
                Text.literal(isEdit ? "✔  Save" : "✔  Create").withColor(0xFF55FF55),
                0xFF0A2A0A, 0xFF0D3A0D, () -> {

                    MinecraftClient _snd = MinecraftClient.getInstance();
                    if (_snd.player != null) {
                        _snd.player.playSound(
                                net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                0.8f, 1.3f);
                    }
                    submitForm();
                }));
        btnRow.child(styledBtn(80, 22,
                Text.literal("✗  Cancel").withColor(0xFFAAAAAA),
                0xFF1A1A2A, 0xFF2A2A3A, () -> { cancelForm(); hub.fillContent(); }));
        panel.child(btnRow);
        panel.child(spacer(16));

        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private void openForm(EventData existing) {
        hub.eventFormOpen = true;
        hub.eventFormInstant = false;
        if (existing == null) {
            hub.eventFormId = ""; hub.eventFormName = ""; hub.eventFormDesc = "";
            hub.eventFormHasLoc = false;
            hub.eventFormLocX = 0; hub.eventFormLocY = 64; hub.eventFormLocZ = 0;
            hub.eventFormEndTime = ""; hub.eventFormSchedStart = "";
        } else {
            hub.eventFormId = existing.id; hub.eventFormName = existing.name;
            hub.eventFormDesc = existing.description != null ? existing.description : "";
            hub.eventFormHasLoc = existing.hasLocation;
            hub.eventFormLocX = existing.locX; hub.eventFormLocY = existing.locY; hub.eventFormLocZ = existing.locZ;
            hub.eventFormEndTime = ""; hub.eventFormSchedStart = "";
        }
        hub.fillContent();
    }

    private void cancelForm() { hub.eventFormOpen = false; hub.eventFormId = ""; hub.eventFormInstant = false; }

    private void submitForm() {

        long endMs;
        long schedMs;
        if (hub.eventFormInstant) {
            long schedDurMs = parseDurationMs(hub.eventFormSchedStart);
            schedMs = schedDurMs > 0 ? System.currentTimeMillis() + schedDurMs : 0;
            endMs   = -1L;
        } else {
            long schedDurMs = parseDurationMs(hub.eventFormSchedStart);
            schedMs = schedDurMs > 0 ? System.currentTimeMillis() + schedDurMs : 0;
            long endDurMs = parseDurationMs(hub.eventFormEndTime);
            endMs = endDurMs > 0
                    ? (schedMs > 0 ? schedMs + endDurMs : System.currentTimeMillis() + endDurMs)
                    : 0;
        }

        boolean isCreate = hub.eventFormId.isEmpty();
        try {
            ClientPlayNetworking.send(new SyncPacket.EventUpsertC2SPayload(
                    isCreate, hub.eventFormId,
                    hub.eventFormName.isBlank() ? "Untitled Event" : hub.eventFormName.trim(),
                    hub.eventFormDesc.trim(),
                    hub.eventFormHasLoc,
                    hub.eventFormLocX, hub.eventFormLocY, hub.eventFormLocZ,
                    endMs, schedMs));
        } catch (Exception ignored) {}
        hub.eventFormOpen = false; hub.eventFormId = ""; hub.eventFormInstant = false;
        hub.fillContent();
    }

    private static FlowLayout pillBadge(String text, int bg, int textColor) {
        FlowLayout badge = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(13));
        badge.surface(Surface.flat(bg));
        badge.padding(Insets.horizontal(5));
        badge.horizontalAlignment(HorizontalAlignment.CENTER);
        badge.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(Text.literal(text).withColor(textColor));
        lbl.shadow(true);
        badge.child(lbl);
        return badge;
    }

    private static FlowLayout infoChip(String text, int bgColor, int textColor) {
        FlowLayout c = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(14));
        c.surface(Surface.flat(bgColor));
        c.padding(Insets.horizontal(6));
        c.verticalAlignment(VerticalAlignment.CENTER);
        c.child(UIComponents.label(Text.literal(text).withColor(textColor)));
        return c;
    }

    static long parseDurationMs(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        raw = raw.trim().toLowerCase();
        try {
            if (raw.endsWith("d")) return Long.parseLong(raw.replace("d", "")) * 86_400_000L;
            if (raw.endsWith("h")) return Long.parseLong(raw.replace("h", "")) * 3_600_000L;
            if (raw.endsWith("m")) return Long.parseLong(raw.replace("m", "")) * 60_000L;
            if (raw.endsWith("s")) return Long.parseLong(raw.replace("s", "")) * 1_000L;
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    static long parseRelativeMs(String raw) {
        long dur = parseDurationMs(raw);
        return dur > 0 ? System.currentTimeMillis() + dur : 0;
    }

    static String fmtDur(long ms) {
        if (ms <= 0) return "now";
        long totalSecs = ms / 1_000;
        if (totalSecs < 60)  return totalSecs + "s";
        long mins    = totalSecs / 60;
        long remSecs = totalSecs % 60;
        if (mins < 10)  return mins + "m " + remSecs + "s";
        if (mins < 60)  return mins + "m";
        long hours   = mins / 60;
        long remMins = mins % 60;
        if (hours < 24) return hours + "h " + remMins + "m";
        return (hours / 24) + "d " + (hours % 24) + "h";
    }

    static String fmtCountdown(long ms) {
        if (ms <= 0) return "now";
        long totalSecs = ms / 1_000;
        if (totalSecs < 60)  return totalSecs + "s";
        long mins    = totalSecs / 60;
        long remSecs = totalSecs % 60;
        if (mins < 5)   return mins + "m " + remSecs + "s";
        if (mins < 60)  return mins + "m";
        long hours   = mins / 60;
        long remMins = mins % 60;
        if (hours < 24) return hours + "h " + remMins + "m";
        return (hours / 24) + "d " + (hours % 24) + "h";
    }

    void updateCountdowns() {
        for (Runnable r : countdownUpdaters) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    private static UUID localUuid() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null ? mc.player.getUuid() : null;
    }

    private FlowLayout buildOpControlsPanel(int innerW) {
        FlowLayout frame = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        frame.surface(Surface.flat(0xFF060810));
        frame.gap(0);

        FlowLayout headerStripe = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(20));
        headerStripe.surface(Surface.flat(0xFF0E1428));
        headerStripe.verticalAlignment(VerticalAlignment.CENTER);
        headerStripe.padding(Insets.horizontal(8));
        headerStripe.gap(6);

        FlowLayout accentBar = UIContainers.verticalFlow(Sizing.fixed(2), Sizing.fixed(12));
        accentBar.surface(Surface.flat(0xFF8855FF));
        headerStripe.child(accentBar);

        LabelComponent headerLbl = UIComponents.label(
                Text.literal("⚙  DIMENSION GATES").withColor(0xFFCCAAFF));
        headerLbl.shadow(true);
        headerStripe.child(headerLbl);

        FlowLayout badge = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(12));
        badge.surface(Surface.flat(0xFF1A0E2A));
        badge.padding(Insets.horizontal(5));
        badge.verticalAlignment(VerticalAlignment.CENTER);
        badge.child(UIComponents.label(Text.literal("OP ONLY").withColor(0xFF8844CC)));
        headerStripe.child(badge);
        frame.child(headerStripe);

        FlowLayout topDivider = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(1));
        topDivider.surface(Surface.flat(0xFF1A1040));
        frame.child(topDivider);

        FlowLayout cardsRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.content());
        cardsRow.gap(1);

        int cardW = innerW / 2;
        cardsRow.child(buildDimGatePanel("nether",
                ClientDimensionGateCache.isNetherOpen(),
                ClientDimensionGateCache.getNetherBaseEnabled(),
                ClientDimensionGateCache.getNetherLinkedEventId(),
                ClientDimensionGateCache.getNetherLinkedEventName(), cardW));

        cardsRow.child(buildDimGatePanel("end",
                ClientDimensionGateCache.isEndOpen(),
                ClientDimensionGateCache.getEndBaseEnabled(),
                ClientDimensionGateCache.getEndLinkedEventId(),
                ClientDimensionGateCache.getEndLinkedEventName(), innerW - cardW - 1));

        frame.child(cardsRow);
        return frame;
    }

    private FlowLayout buildDimGatePanel(String dim, boolean isOpen, boolean baseEnabled,
                                         String linkedEventId, String linkedEventName,
                                         int cardW) {
        boolean isNether  = dim.equals("nether");
        int accentCol = isNether ? 0xFFFF6622 : 0xFF8844EE;
        int labelCol  = isNether ? 0xFFFFCC88 : 0xFFCCAAFF;
        int bgCard    = isNether ? 0xFF120706 : 0xFF08060F;
        String icon   = isNether ? "🔥" : "✦";
        String name   = isNether ? "NETHER"   : "THE END";

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(cardW), Sizing.content());
        card.surface(Surface.flat(bgCard));
        card.padding(Insets.of(6));
        card.gap(4);

        LabelComponent nameLbl = UIComponents.label(
                Text.literal(icon + " " + name).withColor(accentCol));
        nameLbl.shadow(true);
        card.child(nameLbl);

        boolean isEventGated = !linkedEventId.isEmpty();

        if (isEventGated) {

            EventData linkedEv = ClientEventCache.get(linkedEventId);
            boolean eventScheduled = linkedEv != null && linkedEv.isScheduled();
            boolean eventRunning   = linkedEv != null && linkedEv.active && !eventScheduled;

            boolean eventWillOpen = !baseEnabled;

            FlowLayout modeBadge = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(13));
            modeBadge.surface(Surface.flat((accentCol & 0x00FFFFFF) | 0x22000000));
            modeBadge.padding(Insets.horizontal(5));
            modeBadge.verticalAlignment(VerticalAlignment.CENTER);
            String modeLabel = eventWillOpen ? "⏰ OPENS ON EVENT" : "⏰ CLOSES ON EVENT";
            modeBadge.child(UIComponents.label(Text.literal(modeLabel).withColor(accentCol)));
            card.child(modeBadge);

            int statusCol;
            String statusTxt;
            if (eventRunning) {
                if (eventWillOpen) {
                    statusCol = isNether ? 0xFFFF8844 : 0xFFAA66FF;
                    statusTxt = "● OPEN  (event running)";
                } else {
                    statusCol = 0xFF555555;
                    statusTxt = "● CLOSED  (event running)";
                }
            } else if (eventScheduled) {
                statusCol = 0xFFFFAA44;
                long diff0 = linkedEv.scheduledStartMs - System.currentTimeMillis();
                String initialTxt = (eventWillOpen ? "⏳ Opens" : "⏳ Closes") + " in " + fmtCountdown(diff0);
                LabelComponent schedLbl = UIComponents.label(Text.literal(initialTxt).withColor(statusCol));
                card.child(schedLbl);
                final long schedMs2 = linkedEv.scheduledStartMs;
                final boolean willOpen = eventWillOpen;
                countdownUpdaters.add(() -> {
                    long d = schedMs2 - System.currentTimeMillis();
                    if (d > 0) schedLbl.text(Text.literal(
                                    (willOpen ? "⏳ Opens" : "⏳ Closes") + " in " + fmtCountdown(d))
                            .withColor(0xFFFFAA44));
                    else schedLbl.text(Text.literal("⏳ Starting…").withColor(0xFFFFAA44));
                });

                statusTxt = null;
            } else {

                statusCol = baseEnabled ? (isNether ? 0xFFFF8844 : 0xFFAA66FF) : 0xFF555555;
                statusTxt = baseEnabled ? "● OPEN  (waiting for event)" : "● CLOSED  (waiting for event)";
            }
            if (statusTxt != null)
                card.child(UIComponents.label(Text.literal(statusTxt).withColor(statusCol)));

            FlowLayout evRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            evRow.gap(4);
            evRow.verticalAlignment(VerticalAlignment.CENTER);
            evRow.child(UIComponents.label(Text.literal("🔗").withColor(0xFF666677)));
            evRow.child(UIComponents.label(
                    Text.literal(linkedEventName.isEmpty()
                                    ? linkedEventId.substring(0, 8) : linkedEventName)
                            .withColor(labelCol)));
            card.child(evRow);

            FlowLayout line = UIContainers.horizontalFlow(Sizing.fixed(cardW - 12), Sizing.fixed(1));
            line.surface(Surface.flat((accentCol & 0x00FFFFFF) | 0x22000000));
            card.child(line);

            String afterHint = eventWillOpen ? "Stays OPEN after event ends" : "Stays CLOSED after event ends";
            card.child(UIComponents.label(Text.literal(afterHint).withColor(0xFF333344)));

            card.child(styledBtn(cardW - 12, 13,
                    Text.literal("✗ Remove link").withColor(0xFFFFAA44),
                    0xFF1A0E00, 0xFF2A1A00,
                    () -> sendGateAction(dim, baseEnabled, "")));

        } else {

            int chipBg  = isOpen ? ((accentCol & 0x00FFFFFF) | 0x28000000) : 0xFF0E0E0E;
            int chipCol = isOpen ? (isNether ? 0xFFFF8844 : 0xFFAA66FF) : 0xFF444444;
            FlowLayout chip = UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(13));
            chip.surface(Surface.flat(chipBg));
            chip.padding(Insets.horizontal(5));
            chip.verticalAlignment(VerticalAlignment.CENTER);
            chip.child(UIComponents.label(
                    Text.literal(isOpen ? "● OPEN" : "● CLOSED").withColor(chipCol)));
            card.child(chip);

            FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            btnRow.gap(4);
            btnRow.child(styledBtn(44, 14,
                    Text.literal("Open").withColor(isOpen ? 0xFF55FF55 : 0xFF445544),
                    isOpen ? 0xFF0C2A0C : 0xFF151515, 0xFF153A15,
                    () -> sendGateAction(dim, true, "")));
            btnRow.child(styledBtn(46, 14,
                    Text.literal("Close").withColor(!isOpen ? accentCol : 0xFF443322),
                    !isOpen ? (isNether ? 0xFF2A0A00 : 0xFF1A0A2A) : 0xFF151515,
                    isNether ? 0xFF3A1500 : 0xFF2A0A3A,
                    () -> sendGateAction(dim, false, "")));
            card.child(btnRow);

            FlowLayout line = UIContainers.horizontalFlow(Sizing.fixed(cardW - 12), Sizing.fixed(1));
            line.surface(Surface.flat((accentCol & 0x00FFFFFF) | 0x22000000));
            card.child(line);

            List<EventData> activeEvents = ClientEventCache.getActive();
            if (!activeEvents.isEmpty()) {
                String pickerLabel = isOpen ? "Close during event:" : "Open during event:";
                card.child(UIComponents.label(Text.literal(pickerLabel).withColor(0xFF444455)));
                FlowLayout evtFlow = UIContainers.ltrTextFlow(
                        Sizing.fixed(cardW - 12), Sizing.content());
                evtFlow.gap(3);
                for (EventData ev : activeEvents) {
                    final String evId = ev.id;
                    final boolean preState = isOpen;
                    int btnW2 = Math.min(cardW - 12,
                            MinecraftClient.getInstance().textRenderer.getWidth(ev.name) + 12);
                    evtFlow.child(styledBtn(btnW2, 13,
                            Text.literal(ev.name).withColor(labelCol),
                            (accentCol & 0x00FFFFFF) | 0x15000000,
                            (accentCol & 0x00FFFFFF) | 0x28000000,

                            () -> sendGateAction(dim, preState, evId)));
                }
                card.child(evtFlow);
            } else {
                card.child(UIComponents.label(
                        Text.literal("No events to link").withColor(0xFF2A2A33)));
            }
        }

        return card;
    }

    private void sendGateAction(String dimension, boolean enable, String linkedEventId) {
        try {
            ClientPlayNetworking.send(new SyncPacket.DimensionGateActionC2SPayload(
                    dimension, enable, linkedEventId));
        } catch (Exception ignored) {}
    }
}