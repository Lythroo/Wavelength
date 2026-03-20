package com.lythroo.wavelength.common.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientProjectCache {

    private static final Map<String, ProjectData> projects = new ConcurrentHashMap<>();

    private ClientProjectCache() {}

    public static void put(ProjectData project) {
        if (project != null && !project.id.isBlank())
            projects.put(project.id, project);
    }

    public static void remove(String projectId) {
        projects.remove(projectId);
    }

    public static void clear() {
        projects.clear();
    }

    public static ProjectData get(String projectId) { return projects.get(projectId); }

    public static List<ProjectData> getAll() { return new ArrayList<>(projects.values()); }

    public static ProjectData getByOwner(UUID ownerUuid) {
        String id = ownerUuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed && id.equals(p.ownerUuid))
                .findFirst().orElse(null);
    }

    public static List<ProjectData> getOpenProjects(UUID viewerUuid) {
        String id = viewerUuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed)
                .filter(p -> !id.equals(p.ownerUuid))
                .filter(p -> !p.isCollaborator(viewerUuid))
                .sorted(Comparator.comparingLong(p -> -p.startedMs))
                .toList();
    }

    public static List<ProjectData> getCollaborating(UUID localUuid) {
        String id = localUuid.toString();
        return projects.values().stream()
                .filter(p -> !p.completed)
                .filter(p -> p.isCollaborator(localUuid) && !id.equals(p.ownerUuid))
                .toList();
    }
}