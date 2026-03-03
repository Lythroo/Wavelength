package com.lythroo.wavelength.client.render;

public class AnimationHelper {

    public static float sine(long periodMs) {
        double t = (System.currentTimeMillis() % periodMs) / (double) periodMs;
        return (float)(0.5 + 0.5 * Math.sin(t * Math.PI * 2));
    }

    public static float bobOffset(float amplitudeUnits, long periodMs) {
        return (sine(periodMs) * 2f - 1f) * amplitudeUnits;
    }

    public static float spinAngle(long periodMs) {
        return (float)((System.currentTimeMillis() % periodMs) / (double) periodMs * Math.PI * 2);
    }

    public static float pulseAlpha(float minAlpha, long periodMs) {
        return minAlpha + (1f - minAlpha) * sine(periodMs);
    }

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, float factor) {
        int a = (int)(((argb >> 24) & 0xFF) * factor);
        return (argb & 0x00FFFFFF) | (a << 24);
    }
}