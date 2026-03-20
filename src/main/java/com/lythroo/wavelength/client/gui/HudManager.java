package com.lythroo.wavelength.client.gui;

public final class HudManager {

    private static boolean suppressed = false;

    private HudManager() {}

    public static void setSuppressed(boolean value) {
        suppressed = value;
    }

    public static boolean isSuppressed() {
        return suppressed;
    }
}