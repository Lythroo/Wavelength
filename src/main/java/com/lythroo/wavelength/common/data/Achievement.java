package com.lythroo.wavelength.common.data;

import java.util.Map;

public enum Achievement {
    NONE    ("",         0,      0xFFAAAAAA, "○", "Not yet earned.", ""),
    BRONZE  ("Bronze",   1_000,  0xFFCD7F32, "🥉", "A promising start!", "⚡"),
    SILVER  ("Silver",   5_000,  0xFFC0C0C0, "🥈", "Seriously dedicated.", "★"),
    GOLD    ("Gold",     10_000, 0xFFFFD700, "🥇", "A true master of the craft!", "✦"),
    DIAMOND ("Diamond",  50_000, 0xFF00FFFF, "💎", "Legendary. You are the server.", "◆");

    public final String displayName;

    public final int threshold;

    public final int color;

    public final String medal;

    public final String flavorText;

    public final String nameplateSymbol;

    Achievement(String displayName, int threshold, int color,
                String medal, String flavorText, String nameplateSymbol) {
        this.displayName      = displayName;
        this.threshold        = threshold;
        this.color            = color;
        this.medal            = medal;
        this.flavorText       = flavorText;
        this.nameplateSymbol  = nameplateSymbol;
    }

    private static final Map<String, int[]> CATEGORY_THRESHOLDS = Map.of(

            "mining",    new int[]{ 5_000,  25_000,  75_000, 200_000 },
            "building",  new int[]{ 2_000,  10_000,  50_000, 150_000 },
            "combat",    new int[]{   250,   1_000,   5_000,  25_000 },
            "farming",   new int[]{   500,   2_500,  10_000,  50_000 },
            "crafting",  new int[]{   500,   2_500,  10_000,  50_000 },
            "exploring", new int[]{   100,     500,   2_000,  10_000 }
    );

    public static Achievement forCount(long count) {
        if (count < 0) return NONE;
        Achievement best = NONE;
        for (Achievement a : values()) {
            if (count >= a.threshold) best = a;
        }
        return best;
    }

    public static Achievement forCount(long count, String category) {
        if (count < 0) return NONE;
        int[] thresholds = CATEGORY_THRESHOLDS.get(category);
        if (thresholds == null) return forCount(count);

        Achievement[] tiers = { BRONZE, SILVER, GOLD, DIAMOND };
        Achievement best = NONE;
        for (int i = 0; i < tiers.length; i++) {
            if (count >= thresholds[i]) best = tiers[i];
        }
        return best;
    }

    public int thresholdFor(String category) {
        int[] thresholds = CATEGORY_THRESHOLDS.get(category);
        if (thresholds == null) return this.threshold;
        Achievement[] tiers = { BRONZE, SILVER, GOLD, DIAMOND };
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == this) return thresholds[i];
        }
        return 0;
    }

    public int nextThreshold() {
        Achievement[] vals = values();
        int next = this.ordinal() + 1;
        if (next >= vals.length) return -1;
        return vals[next].threshold;
    }

    public int nextThresholdFor(String category) {
        int[] thresholds = CATEGORY_THRESHOLDS.get(category);
        if (thresholds == null) return nextThreshold();
        if (this == NONE) return thresholds[0];
        Achievement[] tiers = { BRONZE, SILVER, GOLD, DIAMOND };
        for (int i = 0; i < tiers.length - 1; i++) {
            if (tiers[i] == this) return thresholds[i + 1];
        }
        return -1;
    }

    public float progressToNext(long count) {
        if (count < 0) return 0f;
        int next = nextThreshold();
        if (next < 0) return 1f;
        float raw = (float)(count - this.threshold) / (float)(next - this.threshold);
        return Math.max(0f, Math.min(1f, raw));
    }

    public float progressToNext(long count, String category) {
        if (count < 0) return 0f;
        int next = nextThresholdFor(category);
        if (next < 0) return 1f;
        int current = thresholdFor(category);
        float raw = (float)(count - current) / (float)(next - current);
        return Math.max(0f, Math.min(1f, raw));
    }

    public int nameplateBorderBase() {
        return switch (this) {
            case GOLD    -> 0xFFCC9900;
            case DIAMOND -> 0xFF00DDEE;
            case SILVER  -> 0xFFAAAAAA;
            case BRONZE  -> 0xFF996633;
            default      -> 0xFF223344;
        };
    }
}