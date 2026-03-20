package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import com.lythroo.wavelength.Wavelength;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public final class DimensionGateManager {

    private static boolean netherEnabled        = true;
    private static boolean endEnabled           = true;
    private static String  netherLinkedEventId  = "";
    private static String  endLinkedEventId     = "";

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_dimension_gates.json";

    private DimensionGateManager() {}

    public static boolean isNetherOpen() {
        if (!netherLinkedEventId.isEmpty()) {

            return netherEnabled;
        }
        return netherEnabled;
    }

    public static boolean isEndOpen() {
        if (!endLinkedEventId.isEmpty()) {
            return endEnabled;
        }
        return endEnabled;
    }

    public static boolean getNetherBaseEnabled()   { return netherEnabled; }
    public static boolean getEndBaseEnabled()      { return endEnabled; }
    public static String  getNetherLinkedEventId() { return netherLinkedEventId; }
    public static String  getEndLinkedEventId()    { return endLinkedEventId; }

    public static void apply(String dimension, boolean enable, String linkedEventId) {
        String safeLink = linkedEventId == null ? "" : linkedEventId;
        switch (dimension) {
            case "nether" -> { netherEnabled = enable; netherLinkedEventId = safeLink; }
            case "end"    -> { endEnabled    = enable; endLinkedEventId    = safeLink; }
            default       -> { return; }
        }
        save();
        Wavelength.LOGGER.info("[Wavelength] Dimension gate updated: {} enabled={} linkedEvent='{}'",
                dimension, enable, safeLink);
    }

    public static void commitEventStart(String eventId) {
        boolean changed = false;
        if (eventId.equals(netherLinkedEventId)) {
            netherEnabled = !netherEnabled;
            netherLinkedEventId = "";
            changed = true;
        }
        if (eventId.equals(endLinkedEventId)) {
            endEnabled = !endEnabled;
            endLinkedEventId = "";
            changed = true;
        }
        if (changed) {
            save();
            Wavelength.LOGGER.info("[Wavelength] Dimension gate committed on event start: " +
                    "nether={} end={}", netherEnabled, endEnabled);
        }
    }

    public static void onEventEnded(String eventId, boolean wasRunning) {
        boolean changed = false;
        if (eventId.equals(netherLinkedEventId)) {
            if (wasRunning) netherEnabled = !netherEnabled;
            netherLinkedEventId = "";
            changed = true;
        }
        if (eventId.equals(endLinkedEventId)) {
            if (wasRunning) endEnabled = !endEnabled;
            endLinkedEventId = "";
            changed = true;
        }
        if (changed) {
            save();
            Wavelength.LOGGER.info("[Wavelength] Dimension gate event ended (wasRunning={}): " +
                    "nether={} end={}", wasRunning, netherEnabled, endEnabled);
        }
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!path.toFile().exists()) { save(); return; }
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("netherEnabled"))     netherEnabled       = root.get("netherEnabled").getAsBoolean();
            if (root.has("endEnabled"))        endEnabled          = root.get("endEnabled").getAsBoolean();
            if (root.has("netherLinkedEvent")) netherLinkedEventId = root.get("netherLinkedEvent").getAsString();
            if (root.has("endLinkedEvent"))    endLinkedEventId    = root.get("endLinkedEvent").getAsString();
            Wavelength.LOGGER.info("[Wavelength] Dimension gates loaded: nether={} end={}", netherEnabled, endEnabled);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load dimension gates: {}", e.getMessage());
        }
    }

    public static void saveNow() { save(); }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("netherEnabled",     netherEnabled);
        root.addProperty("endEnabled",        endEnabled);
        root.addProperty("netherLinkedEvent", netherLinkedEventId);
        root.addProperty("endLinkedEvent",    endLinkedEventId);
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (Writer w = new FileWriter(path.toFile())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to save dimension gates: {}", e.getMessage());
        }
    }
}