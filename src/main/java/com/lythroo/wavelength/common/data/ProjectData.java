package com.lythroo.wavelength.common.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectData {

    public String id          = "";
    public String ownerUuid   = "";
    public String ownerName   = "";
    public String name        = "";
    public String description = "";

    public int progress = 0;

    public List<Material> materials = new ArrayList<>();

    public static class Material {
        public String name     = "";
        public int    required = 0;
        public int    current  = 0;

        public Material() {}
        public Material(String name, int required, int current) {
            this.name = name; this.required = required; this.current = current;
        }

        public String icon() {
            if (current >= required) return "✓";
            if (current > 0)         return "⚠";
            return "✗";
        }

        public int iconColor() {
            if (current >= required) return 0xFF55FF55;
            if (current > 0)         return 0xFFFFAA00;
            return 0xFFFF5555;
        }
    }

    public boolean hasLocation = false;
    public long    locX = 0, locY = 64, locZ = 0;

    public String collaboration = "SOLO";

    public List<Collaborator> collaborators = new ArrayList<>();

    public static class Collaborator {
        public String uuid              = "";
        public String name              = "";
        public long   blocksContributed = 0;

        public Collaborator() {}
        public Collaborator(String uuid, String name, long blocks) {
            this.uuid = uuid; this.name = name; this.blocksContributed = blocks;
        }
    }

    public List<PendingJoin> pendingJoins = new ArrayList<>();

    public static class PendingJoin {
        public String uuid = "";
        public String name = "";
        public PendingJoin() {}
        public PendingJoin(String uuid, String name) { this.uuid = uuid; this.name = name; }
    }

    public long    startedMs = System.currentTimeMillis();
    public boolean completed = false;

    public boolean isSolo()        { return "SOLO".equals(collaboration); }
    public boolean isHelpWanted()  { return "HELP_WANTED".equals(collaboration); }
    public boolean isTeamProject() { return "TEAM".equals(collaboration); }

    public boolean isOwner(UUID uuid) {
        return uuid.toString().equals(ownerUuid);
    }

    public boolean isCollaborator(UUID uuid) {
        String id = uuid.toString();
        return collaborators.stream().anyMatch(c -> id.equals(c.uuid));
    }

    public Collaborator getCollaborator(UUID uuid) {
        String id = uuid.toString();
        return collaborators.stream().filter(c -> id.equals(c.uuid)).findFirst().orElse(null);
    }

    public boolean hasPendingFrom(UUID uuid) {
        String id = uuid.toString();
        return pendingJoins.stream().anyMatch(p -> id.equals(p.uuid));
    }

    public String collaborationLabel() {
        return switch (collaboration) {
            case "HELP_WANTED" -> "Help Wanted";
            case "TEAM"        -> "Team Project";
            default            -> "Solo";
        };
    }

    public String timeAgo() {
        long diff  = System.currentTimeMillis() - startedMs;
        long secs  = diff / 1_000;
        if (secs < 60)   return secs + "s ago";
        long mins  = secs / 60;
        if (mins < 60)   return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24)  return hours + "h ago";
        return (hours / 24) + "d ago";
    }
}