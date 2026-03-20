package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import com.lythroo.wavelength.Wavelength;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerEventData {

    private static final Map<String, EventData> events = new ConcurrentHashMap<>();

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_server_events.json";

    private ServerEventData() {}

    public static void load() {
        events.clear();
        Path path = savePath();
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray arr   = root.getAsJsonArray("events");
            if (arr != null) {
                for (JsonElement el : arr) {
                    try {
                        EventData e = GSON.fromJson(el, EventData.class);

                        if (e != null && e.id != null && !e.id.isBlank() && e.active)
                            events.put(e.id, e);
                    } catch (Exception ignored) {}
                }
            }
            Wavelength.LOGGER.info("[Wavelength] Loaded {} event(s).", events.size());
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load events: {}", e.getMessage());
        }
    }

    public static void saveNow() { save(); }

    private static void save() {
        JsonArray arr = new JsonArray();

        events.values().stream().filter(e -> e.active).forEach(e -> arr.add(GSON.toJsonTree(e)));
        JsonObject root = new JsonObject();
        root.add("events", arr);
        Path target = savePath();
        Path tmp    = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer w = new FileWriter(tmp.toFile())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to write events tmp: {}", e.getMessage());
            return;
        }
        try {
            java.nio.file.Files.move(tmp, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to atomically replace events file: {}", e.getMessage());
        }
    }

    public static EventData get(String id) { return events.get(id); }

    public static List<EventData> getActive() {
        return events.values().stream().filter(e -> e.active).toList();
    }

    public static List<EventData> getByOwner(UUID ownerUuid) {
        String id = ownerUuid.toString();
        return events.values().stream()
                .filter(e -> e.active && id.equals(e.ownerUuid))
                .toList();
    }

    public static EventData create(UUID ownerUuid, String ownerName,
                                   String name, String description,
                                   boolean hasLocation, long locX, long locY, long locZ,
                                   long endMs, long scheduledStartMs) {
        if (getByOwner(ownerUuid).size() >= EventData.MAX_EVENTS_PER_PLAYER) return null;

        EventData e     = new EventData();
        e.id            = UUID.randomUUID().toString();
        e.ownerUuid     = ownerUuid.toString();
        e.ownerName     = ownerName;
        e.name          = name.isBlank() ? "Untitled Event" : name;
        e.description   = description;
        e.hasLocation   = hasLocation;
        e.locX = locX; e.locY = locY; e.locZ = locZ;
        e.startMs           = System.currentTimeMillis();
        e.scheduledStartMs  = scheduledStartMs;
        e.endMs         = endMs;
        e.active        = true;

        events.put(e.id, e);
        save();
        return e;
    }

    public static boolean update(String eventId, UUID requesterUuid,
                                 String name, String description,
                                 boolean hasLocation, long locX, long locY, long locZ,
                                 long endMs, long scheduledStartMs) {
        EventData e = events.get(eventId);
        if (e == null || !e.ownerUuid.equals(requesterUuid.toString()) || !e.active) return false;
        e.name              = name.isBlank() ? "Untitled Event" : name;
        e.description       = description;
        e.hasLocation       = hasLocation;
        e.locX = locX; e.locY = locY; e.locZ = locZ;
        e.endMs             = endMs;
        e.scheduledStartMs  = scheduledStartMs;
        save();
        return true;
    }

    public static boolean delete(String eventId, UUID requesterUuid) {
        EventData e = events.get(eventId);
        if (e == null || !e.ownerUuid.equals(requesterUuid.toString())) return false;
        events.remove(eventId);
        save();
        return true;
    }

    public static boolean adminDelete(String eventId) {
        if (!events.containsKey(eventId)) return false;
        events.remove(eventId);
        save();
        return true;
    }

    public static boolean end(String eventId, UUID requesterUuid) {
        EventData e = events.get(eventId);
        if (e == null || !e.ownerUuid.equals(requesterUuid.toString()) || !e.active) return false;
        e.active = false;
        e.endMs  = System.currentTimeMillis();
        save();
        return true;
    }

    private static Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}