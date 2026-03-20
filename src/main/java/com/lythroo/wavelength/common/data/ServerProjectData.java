package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import com.lythroo.wavelength.Wavelength;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerProjectData {

    private static final Map<String, ProjectData> projects = new ConcurrentHashMap<>();

    private static volatile boolean dirty = false;

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_server_projects.json";

    private ServerProjectData() {}

    public static void load() {
        projects.clear();
        Path path = savePath();
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray  arr  = root.getAsJsonArray("projects");
            if (arr != null) {
                for (JsonElement el : arr) {
                    try {
                        ProjectData p = GSON.fromJson(el, ProjectData.class);

                        if (p != null && p.id != null && !p.id.isBlank() && !p.completed)
                            projects.put(p.id, p);
                    } catch (Exception ignored) {}
                }
            }
            Wavelength.LOGGER.info("[Wavelength] Loaded {} project(s).", projects.size());
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load projects: {}", e.getMessage());
        }
    }

    public static void savePeriodic() {
        if (dirty) { save(); dirty = false; }
    }

    public static void saveNow() { save(); dirty = false; }

    private static void save() {
        JsonArray arr = new JsonArray();
        projects.values().forEach(p -> arr.add(GSON.toJsonTree(p)));
        JsonObject root = new JsonObject();
        root.add("projects", arr);
        Path target = savePath();
        Path tmp    = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer w = new FileWriter(tmp.toFile())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to write projects tmp: {}", e.getMessage());
            return;
        }
        try {
            java.nio.file.Files.move(tmp, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to atomically replace projects file: {}", e.getMessage());
        }
    }

    public static ProjectData get(String projectId) { return projects.get(projectId); }

    public static List<ProjectData> getActive() {
        return projects.values().stream().filter(p -> !p.completed).toList();
    }

    public static ProjectData getByOwner(UUID ownerUuid) {
        String id = ownerUuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed && id.equals(p.ownerUuid))
                .findFirst().orElse(null);
    }

    public static ProjectData getActiveProjectFor(UUID uuid) {
        String id = uuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed)
                .filter(p -> id.equals(p.ownerUuid) || p.isCollaborator(uuid))
                .findFirst().orElse(null);
    }

    public static List<ProjectData> getOpenProjects(UUID viewerUuid) {
        String id = viewerUuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed)
                .filter(p -> p.isHelpWanted() || p.isTeamProject())
                .filter(p -> !id.equals(p.ownerUuid))
                .filter(p -> !p.isCollaborator(viewerUuid))
                .sorted(Comparator.comparingLong(p -> -p.startedMs))
                .toList();
    }

    public static ProjectData create(UUID ownerUuid, String ownerName,
                                     String name, String description,
                                     int progress,
                                     List<ProjectData.Material> materials,
                                     boolean hasLocation, long locX, long locY, long locZ,
                                     String collaboration) {
        if (getByOwner(ownerUuid) != null) return null;

        ProjectData p   = new ProjectData();
        p.id            = UUID.randomUUID().toString();
        p.ownerUuid     = ownerUuid.toString();
        p.ownerName     = ownerName;
        p.name          = name.isBlank() ? "Untitled Project" : name;
        p.description   = description;
        p.progress      = Math.max(0, Math.min(100, progress));
        p.materials     = materials != null ? new ArrayList<>(materials) : new ArrayList<>();
        p.hasLocation   = hasLocation;
        p.locX = locX; p.locY = locY; p.locZ = locZ;
        p.collaboration = collaboration;
        p.startedMs     = System.currentTimeMillis();
        p.collaborators.add(new ProjectData.Collaborator(ownerUuid.toString(), ownerName, 0));

        projects.put(p.id, p);
        save();
        return p;
    }

    public static boolean update(String projectId, UUID requesterUuid,
                                 String name, String description, int progress,
                                 List<ProjectData.Material> materials,
                                 boolean hasLocation, long locX, long locY, long locZ,
                                 String collaboration) {
        ProjectData p = projects.get(projectId);
        if (p == null || !p.ownerUuid.equals(requesterUuid.toString()) || p.completed) return false;
        p.name          = name.isBlank() ? "Untitled Project" : name;
        p.description   = description;
        p.progress      = Math.max(0, Math.min(100, progress));
        p.materials     = new ArrayList<>(materials);
        p.hasLocation   = hasLocation;
        p.locX = locX; p.locY = locY; p.locZ = locZ;
        p.collaboration = collaboration;
        save();
        return true;
    }

    public static boolean delete(String projectId, UUID requesterUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null || !p.ownerUuid.equals(requesterUuid.toString())) return false;
        projects.remove(projectId);
        save();
        return true;
    }

    public static boolean adminDelete(String projectId) {
        if (!projects.containsKey(projectId)) return false;
        projects.remove(projectId);
        save();
        return true;
    }

    public static boolean addJoinRequest(String projectId, UUID joinerUuid, String joinerName) {
        ProjectData p = projects.get(projectId);
        if (p == null || p.completed) return false;
        if (p.ownerUuid.equals(joinerUuid.toString())) return false;
        if (p.isCollaborator(joinerUuid)) return false;
        if (p.hasPendingFrom(joinerUuid)) return false;
        p.pendingJoins.add(new ProjectData.PendingJoin(joinerUuid.toString(), joinerName));
        save();
        return true;
    }

    public static boolean acceptJoin(String projectId, UUID ownerUuid,
                                     UUID joinerUuid, String joinerName) {
        ProjectData p = projects.get(projectId);
        if (p == null || !p.ownerUuid.equals(ownerUuid.toString())) return false;
        p.pendingJoins.removeIf(j -> j.uuid.equals(joinerUuid.toString()));
        if (!p.isCollaborator(joinerUuid))
            p.collaborators.add(new ProjectData.Collaborator(joinerUuid.toString(), joinerName, 0));
        save();
        return true;
    }

    public static boolean declineJoin(String projectId, UUID ownerUuid, UUID joinerUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null || !p.ownerUuid.equals(ownerUuid.toString())) return false;
        boolean removed = p.pendingJoins.removeIf(j -> j.uuid.equals(joinerUuid.toString()));
        if (removed) save();
        return removed;
    }

    public static boolean leaveProject(String projectId, UUID collaboratorUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null) return false;
        if (p.ownerUuid.equals(collaboratorUuid.toString())) return false;
        boolean removed = p.collaborators.removeIf(c -> c.uuid.equals(collaboratorUuid.toString()));
        if (removed) save();
        return removed;
    }

    public static boolean kickCollaborator(String projectId, UUID ownerUuid, UUID targetUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null || p.completed) return false;
        if (!p.ownerUuid.equals(ownerUuid.toString())) return false;
        if (p.ownerUuid.equals(targetUuid.toString())) return false;
        boolean removed = p.collaborators.removeIf(c -> c.uuid.equals(targetUuid.toString()));
        if (removed) save();
        return removed;
    }

    public static boolean complete(String projectId, UUID ownerUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null || !p.ownerUuid.equals(ownerUuid.toString()) || p.completed) return false;
        p.completed = true;
        p.progress  = 100;
        save();

        projects.remove(projectId);
        return true;
    }

    public static void incrementContribution(String projectId, UUID playerUuid) {
        ProjectData p = projects.get(projectId);
        if (p == null) return;
        ProjectData.Collaborator c = p.getCollaborator(playerUuid);
        if (c != null) { c.blocksContributed++; dirty = true; }
    }

    private static Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}