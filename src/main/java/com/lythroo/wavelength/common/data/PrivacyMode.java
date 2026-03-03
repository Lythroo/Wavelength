package com.lythroo.wavelength.common.data;

public enum PrivacyMode {
    PUBLIC  ("Public",       0xFF55FF55),
    FRIENDS ("Friends Only", 0xFFFFFF55),
    GHOST   ("Ghost Mode",   0xFFFF5555);

    public final String displayName;

    public final int dotColor;

    PrivacyMode(String displayName, int dotColor) {
        this.displayName = displayName;
        this.dotColor = dotColor;
    }

    public PrivacyMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}