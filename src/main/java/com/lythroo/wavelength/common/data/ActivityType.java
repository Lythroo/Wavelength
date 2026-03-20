package com.lythroo.wavelength.common.data;

public enum ActivityType {
    MINING   ("Mining",    "[Mining]",    0xFFAA8855),
    BUILDING ("Building",  "[Building]",  0xFF88AAFF),
    FIGHTING ("Fighting",  "[Fighting]",  0xFFFF5555),
    FARMING  ("Farming",   "[Farming]",   0xFF55FF55),
    EXPLORING("Exploring", "[Exploring]", 0xFF55FFFF),
    AFK      ("AFK",       "[  ZZZ  ]",   0xFF888888),
    IDLE     ("Idle",      "",            0xFFCCCCCC);

    public final String displayName;

    public final String tag;

    public final int color;

    ActivityType(String displayName, String tag, int color) {
        this.displayName = displayName;
        this.tag = tag;
        this.color = color;
    }
}