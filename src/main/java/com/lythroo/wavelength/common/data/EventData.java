package com.lythroo.wavelength.common.data;

public class EventData {

    public static final int MAX_EVENTS_PER_PLAYER = 2;

    public String id        = "";
    public String ownerUuid = "";
    public String ownerName = "";
    public String name      = "";
    public String description = "";

    public boolean hasLocation = false;
    public long    locX = 0, locY = 64, locZ = 0;

    public long    startMs          = System.currentTimeMillis();
    public long    scheduledStartMs = 0;
    public long    endMs            = 0;
    public boolean active           = true;

    public boolean isOwner(java.util.UUID uuid) {
        return uuid.toString().equals(ownerUuid);
    }

    public boolean isScheduled() {
        return scheduledStartMs > 0 && scheduledStartMs > System.currentTimeMillis();
    }

    public String timeUntilStart() {
        if (!isScheduled()) return "";
        long diff = scheduledStartMs - System.currentTimeMillis();
        long secs = diff / 1_000;
        long mins = secs / 60;
        if (mins < 60) return mins + "m";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    public String timeUntilEnd() {
        if (endMs <= 0) return "";
        long diff = endMs - System.currentTimeMillis();
        if (diff <= 0) return "ended";
        long secs  = diff / 1_000;
        long mins  = secs / 60;
        if (mins < 60) return mins + "m";
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    public String timeAgo() {
        long diff = System.currentTimeMillis() - startMs;
        long secs = diff / 1_000;
        if (secs < 60)           return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60)           return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24)          return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}