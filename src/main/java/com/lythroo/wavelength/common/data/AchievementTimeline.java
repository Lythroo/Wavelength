package com.lythroo.wavelength.common.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AchievementTimeline {

    public static final int MAX_EVENTS = 50;

    public record TimelineEvent(
            UUID        playerUuid,
            String      playerName,
            String      category,
            Achievement badge,
            long        timestampMs
    ) {

        public String timeAgo() {
            long diff  = System.currentTimeMillis() - timestampMs;
            long secs  = diff / 1_000;
            if (secs < 60)              return secs + "s ago";
            long mins  = secs  / 60;
            if (mins < 60)              return mins + "m ago";
            long hours = mins  / 60;
            if (hours < 24)             return hours + "h ago";
            return (hours / 24) + "d ago";
        }

        public String description() {
            String icon = switch (badge) {
                case DIAMOND -> " \uD83D\uDC8E";
                case GOLD    -> " \uD83E\uDD47";
                case SILVER  -> " \uD83E\uDD48";
                case BRONZE  -> " \uD83E\uDD49";
                default      -> "";
            };
            return playerName + " unlocked " + badge.displayName
                    + " " + capitalize(category) + "!" + icon;
        }

        private static String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    private static final List<TimelineEvent> events = new CopyOnWriteArrayList<>();

    private static final Map<UUID, Map<String, Achievement>> lastBadges =
            new ConcurrentHashMap<>();

    private static final String[] CATEGORIES =
            {"mining", "building", "combat", "farming", "crafting", "exploring"};

    private AchievementTimeline() {}

    public static void checkAndRecord(UUID uuid, String name,
                                      long statMined,   long statPlaced,
                                      long statKilled,  long statCrops,
                                      long statCrafted, long statDistM) {
        long[] vals = {statMined, statPlaced, statKilled, statCrops, statCrafted, statDistM};

        Map<String, Achievement> prev =
                lastBadges.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        for (int i = 0; i < CATEGORIES.length; i++) {
            String      cat     = CATEGORIES[i];
            Achievement current = Achievement.forCount(vals[i], cat);
            Achievement last    = prev.get(cat);

            if (last == null) {

                prev.put(cat, current);
            } else if (current.ordinal() > last.ordinal()) {
                prev.put(cat, current);
                addEvent(new TimelineEvent(uuid, name, cat, current,
                        System.currentTimeMillis()));
            }
        }
    }

    public static void addEvent(TimelineEvent event) {
        events.add(0, event);

        while (events.size() > MAX_EVENTS) events.remove(events.size() - 1);
    }

    public static List<TimelineEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public static void removePlayer(UUID uuid) {
        lastBadges.remove(uuid);
    }

    public record ProjectTimelineEvent(
            UUID   ownerUuid,
            String ownerName,
            String projectName,
            String action,
            String extraName,
            long   timestampMs
    ) {
        public String timeAgo() {
            long diff = System.currentTimeMillis() - timestampMs;
            long secs = diff / 1_000;
            if (secs < 60)            return secs + "s ago";
            long mins = secs / 60;
            if (mins < 60)            return mins + "m ago";
            long hours = mins / 60;
            if (hours < 24)           return hours + "h ago";
            return (hours / 24) + "d ago";
        }

        public String description() {
            return switch (action) {
                case "CREATED"            -> ownerName + " started a new project: \"" + projectName + "\"";
                case "HELP_WANTED"        -> ownerName + " is looking for help on \"" + projectName + "\"";
                case "COLLABORATOR_JOINED"-> extraName + " joined \"" + projectName + "\"";
                case "COLLABORATOR_LEFT"  -> extraName + " left \"" + projectName + "\"";
                case "COMPLETED"          -> "🎉 \"" + projectName + "\" completed by " + ownerName + "!";
                case "EVENT_CREATED"      -> "📅 " + ownerName + " created event: \"" + projectName + "\"";
                case "EVENT_STARTED"      -> "🔔 " + ownerName + "'s event \"" + projectName + "\" has begun!";
                case "EVENT_ENDED"        -> "✓ " + ownerName + "'s event \"" + projectName + "\" has ended";
                default                   -> ownerName + ": \"" + projectName + "\" [" + action + "]";
            };
        }

        public int accentColor() {
            return switch (action) {
                case "CREATED"            -> 0xFF5588FF;
                case "HELP_WANTED"        -> 0xFFFFAA00;
                case "COLLABORATOR_JOINED"-> 0xFF55FF88;
                case "COLLABORATOR_LEFT"  -> 0xFFFF6655;
                case "COMPLETED"          -> 0xFFFFD700;
                case "EVENT_CREATED"      -> 0xFF55DDFF;
                case "EVENT_STARTED"      -> 0xFF55FFBB;
                case "EVENT_ENDED"        -> 0xFF7799BB;
                default                   -> 0xFF888888;
            };
        }

        public int cardBg() {
            return switch (action) {
                case "CREATED"            -> 0xCC0A0E1A;
                case "HELP_WANTED"        -> 0xCC1A1000;
                case "COLLABORATOR_JOINED"-> 0xCC061206;
                case "COLLABORATOR_LEFT"  -> 0xCC160606;
                case "COMPLETED"          -> 0xCC130F00;
                case "EVENT_CREATED"      -> 0xCC060D14;
                case "EVENT_STARTED"      -> 0xCC061410;
                case "EVENT_ENDED"        -> 0xCC0A0D14;
                default                   -> 0xCC0D0D1A;
            };
        }
    }

    private static final List<ProjectTimelineEvent> projectEvents = new CopyOnWriteArrayList<>();

    public static void addProjectEvent(ProjectTimelineEvent event) {
        projectEvents.add(0, event);
        while (projectEvents.size() > MAX_EVENTS) projectEvents.remove(projectEvents.size() - 1);
    }

    public static List<ProjectTimelineEvent> getProjectEvents() {
        return Collections.unmodifiableList(projectEvents);
    }

    public static void clear() {
        events.clear();
        projectEvents.clear();
        lastBadges.clear();
    }
}