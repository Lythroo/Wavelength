package com.lythroo.wavelength.common.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientEventCache {

    private static final Map<String, EventData> events = new ConcurrentHashMap<>();

    private ClientEventCache() {}

    public static void put(EventData event) {
        if (event != null && !event.id.isBlank())
            events.put(event.id, event);
    }

    public static void remove(String eventId) {
        events.remove(eventId);
    }

    public static void clear() {
        events.clear();
    }

    public static EventData get(String id) { return events.get(id); }

    public static List<EventData> getActive() {
        return events.values().stream()
                .filter(e -> e.active)
                .sorted(Comparator.comparingLong(e -> -e.startMs))
                .toList();
    }

    public static List<EventData> getByOwner(UUID ownerUuid) {
        String id = ownerUuid.toString();
        return events.values().stream()
                .filter(e -> e.active && id.equals(e.ownerUuid))
                .toList();
    }

    public static int countByOwner(UUID ownerUuid) {
        return getByOwner(ownerUuid).size();
    }
}