package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.*;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.UUID;

public final class HubUiHelper {

    static final int COL_BG          = 0xDD0D0D1A;
    static final int COL_HEADER_TXT  = 0xFFCCCCFF;
    static final int COL_GOLD        = 0xFFFFD700;
    static final int COL_GRAY        = 0xFFAAAAAA;
    static final int COL_DIVIDER     = 0x55FFFFFF;
    static final int COL_TAB_ACTIVE  = 0xFF5555AA;
    static final int COL_TAB_IDLE    = 0xFF2A2A55;
    static final int COL_BAR_BG      = 0xFF333333;
    static final int COL_SORT_ACTIVE = 0xFF665599;
    static final int COL_SORT_IDLE   = 0xFF2A2A44;

    static final String[] CATS   = {"mining","building","combat","farming","crafting","exploring"};
    static final String[] LABELS = {"⛏ Mining","🏗 Building","⚔ Combat","🌾 Farming","🔨 Crafting","🧭 Exploring"};
    static final String[] SHORT  = {"⛏ Mining","🏗 Build","⚔ Combat","🌾 Farm","🔨 Craft","🧭 Explore"};
    static final int[]    COLORS = {0xFFAA8855, 0xFF88AAFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFAA00, 0xFF55FFFF};

    private HubUiHelper() {}

    static FlowLayout styledBtn(int w, int h, Text label, int normalBg, int hoverBg, Runnable action) {
        FlowLayout btn = UIContainers.horizontalFlow(Sizing.fixed(w), Sizing.fixed(h));
        btn.surface(Surface.flat(normalBg));
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(label);
        lbl.shadow(true);
        btn.child(lbl);
        btn.mouseEnter().subscribe(() -> btn.surface(Surface.flat(hoverBg)));
        btn.mouseLeave().subscribe(() -> btn.surface(Surface.flat(normalBg)));
        btn.mouseDown().subscribe((x, y) -> {
            playClick();
            MinecraftClient.getInstance().execute(action);
            return true;
        });
        return btn;
    }

    static LabelComponent sectionHeader(String text) {
        LabelComponent l = UIComponents.label(Text.literal(text).withColor(COL_HEADER_TXT));
        l.shadow(true);
        return l;
    }

    static FlowLayout divider(int panelW) {
        FlowLayout d = UIContainers.horizontalFlow(Sizing.fixed(panelW - 32), Sizing.fixed(1));
        d.surface(Surface.flat(COL_DIVIDER));
        d.margins(Insets.vertical(3));
        return d;
    }

    static UIComponent spacer(int h) {
        return UIComponents.label(Text.literal("")).sizing(Sizing.fixed(10), Sizing.fixed(h));
    }

    static FlowLayout statLine(String label, String value, int panelW) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(panelW - 32), Sizing.content());
        row.gap(6);
        row.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(Text.literal(label + ":").withColor(COL_GRAY));
        lbl.sizing(Sizing.fixed(panelW - 178), Sizing.content());
        row.child(lbl);
        row.child(UIComponents.label(Text.literal(value).withColor(0xFFDDDDDD)));
        return row;
    }

    static FlowLayout sectionDividerLabel(String text, int color) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20));
        row.padding(Insets.of(4, 0, 2, 4));
        row.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(Text.literal(text).withColor(color));
        lbl.shadow(true);
        row.child(lbl);
        return row;
    }

    static void playClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    static void playTabClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.4f);
    }

    static void playSuccess() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    static void playNegative() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.7f);
    }

    static void playTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.5f);
    }

    static String formatNum(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    static String formatDist(long rawCm) {
        if (rawCm < 100_000) return formatNum(rawCm / 100) + " m";
        return formatNum(rawCm / 100_000) + " km";
    }

    static String formatDuration(long sec) {
        if (sec < 60)   return sec + "s";
        if (sec < 3600) return (sec / 60) + "m";
        return (sec / 3600) + "h " + ((sec % 3600) / 60) + "m";
    }

    static String formatLeaderboardValue(long value, String cat) {
        return switch (cat) {
            case "exploring" -> formatDist(value * 1000);
            case "combat"    -> formatNum(value) + " kills";
            case "farming"   -> formatNum(value) + " crops";
            case "crafting"  -> formatNum(value) + " items";
            default          -> formatNum(value) + " blocks";
        };
    }

    static String privacyDot(PrivacyMode p) {
        return switch (p) { case PUBLIC -> "🟢"; case FRIENDS -> "🟡"; case GHOST -> "🔴"; };
    }

    public static void spawnCompletionParticles() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 40; i++) {
            double vx = (rng.nextDouble() - 0.5) * 0.8;
            double vy = rng.nextDouble() * 0.55 + 0.1;
            double vz = (rng.nextDouble() - 0.5) * 0.8;
            mc.world.addParticleClient(ParticleTypes.END_ROD, true, false, px, py + 1.0, pz, vx, vy, vz);
        }
        for (int i = 0; i < 25; i++) {
            double angle = (i / 25.0) * Math.PI * 2;
            mc.world.addParticleClient(ParticleTypes.ENCHANT, true, false,
                    px + Math.cos(angle) * 0.5, py + 1.8, pz + Math.sin(angle) * 0.5,
                    Math.cos(angle) * 0.2, 0.12, Math.sin(angle) * 0.2);
        }
    }

    static Achievement computeTopBadgeFromCache(PersistentPlayerCache.Entry e) {
        if (e == null) return Achievement.NONE;
        Achievement best = Achievement.NONE;
        long[] vals = {e.blocksMined, e.blocksPlaced, e.mobsKilled,
                e.cropsHarvested, e.itemsCrafted, e.distanceTraveled / 1000};
        for (int i = 0; i < CATS.length; i++) {
            Achievement a = Achievement.forCount(vals[i], CATS[i]);
            if (a.ordinal() > best.ordinal()) best = a;
        }
        return best;
    }

    static String resolveUsername(UUID uuid, MinecraftClient client) {
        if (client.world != null) {
            for (var p : client.world.getPlayers()) {
                if (p.getUuid().equals(uuid)) return p.getName().getString();
            }
        }
        PersistentPlayerCache.Entry cached = PersistentPlayerCache.get(uuid);
        if (cached != null && !cached.username.isBlank()) return cached.username;
        return uuid.toString().substring(0, 8);
    }
}