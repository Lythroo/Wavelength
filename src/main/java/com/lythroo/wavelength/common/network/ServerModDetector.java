package com.lythroo.wavelength.common.network;

import com.lythroo.wavelength.Wavelength;

public final class ServerModDetector {

    private static boolean serverModPresent = false;

    private ServerModDetector() {}

    public static void markPresent() {
        if (!serverModPresent) {
            serverModPresent = true;
            Wavelength.LOGGER.info("[Wavelength] ✔ Server mod confirmed (received S2C data).");
            Wavelength.LOGGER.info("[Wavelength]   Cross-player sync is ACTIVE.");
        }
    }

    public static boolean isPresent() {
        return serverModPresent;
    }

    public static void reset() {
        serverModPresent = false;
        Wavelength.LOGGER.info("[Wavelength] Server mod state reset (disconnected).");
    }
}