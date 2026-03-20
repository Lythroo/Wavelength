package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendRequests {

    public record PendingRequest(String uuid, String username) {}

    private static final List<PendingRequest> incoming = new CopyOnWriteArrayList<>();
    private static final List<PendingRequest> outgoing = new CopyOnWriteArrayList<>();

    private static String ownerUuid = null;

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_friend_requests.json";

    public static void onPlayerJoin(UUID localPlayerUuid) {
        String id = localPlayerUuid.toString();
        if (!id.equals(ownerUuid)) {
            incoming.clear();
            outgoing.clear();
            ownerUuid = id;
            loadFromDisk();
        }
    }

    public static void clearAll() {
        incoming.clear();
        outgoing.clear();

        ownerUuid = null;
    }

    public static List<PendingRequest> getIncoming() {
        return Collections.unmodifiableList(incoming);
    }

    public static List<PendingRequest> getOutgoing() {
        return Collections.unmodifiableList(outgoing);
    }

    public static boolean hasIncomingFrom(UUID uuid) {
        String id = uuid.toString();
        return incoming.stream().anyMatch(r -> r.uuid().equals(id));
    }

    public static boolean hasSentTo(UUID uuid) {
        String id = uuid.toString();
        return outgoing.stream().anyMatch(r -> r.uuid().equals(id));
    }

    public static int incomingCount() {
        return incoming.size();
    }

    public static void addIncoming(UUID uuid, String username) {
        if (!hasIncomingFrom(uuid)) {
            incoming.add(new PendingRequest(uuid.toString(), username));
            save();
        }
    }

    public static void removeIncoming(UUID uuid) {
        if (incoming.removeIf(r -> r.uuid().equals(uuid.toString()))) save();
    }

    public static void addOutgoing(UUID uuid, String username) {
        if (!hasSentTo(uuid)) {
            outgoing.add(new PendingRequest(uuid.toString(), username));
            save();
        }
    }

    public static void removeOutgoing(UUID uuid) {
        if (outgoing.removeIf(r -> r.uuid().equals(uuid.toString()))) save();
    }

    public static void cleanConfirmedFriends(List<String> confirmedFriendUuids) {
        if (confirmedFriendUuids == null || confirmedFriendUuids.isEmpty()) return;
        Set<String> friendSet = new HashSet<>(confirmedFriendUuids);
        boolean changed = false;
        changed |= incoming.removeIf(r -> friendSet.contains(r.uuid()));
        changed |= outgoing.removeIf(r -> friendSet.contains(r.uuid()));
        if (changed) save();
    }

    private static void loadFromDisk() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            String storedOwner = root.has("ownerUuid") ? root.get("ownerUuid").getAsString() : "";
            if (!storedOwner.equals(ownerUuid)) {
                return;
            }
            parseList(root.getAsJsonArray("incoming"), incoming);
            parseList(root.getAsJsonArray("outgoing"), outgoing);
        } catch (Exception ignored) {}
    }

    private static void parseList(JsonArray arr, List<PendingRequest> list) {
        if (arr == null) return;
        for (JsonElement el : arr) {
            try {
                JsonObject obj = el.getAsJsonObject();
                list.add(new PendingRequest(
                        obj.get("uuid").getAsString(),
                        obj.get("username").getAsString()));
            } catch (Exception ignored) {}
        }
    }

    private static void save() {
        if (ownerUuid == null) return;
        Path target = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        root.addProperty("ownerUuid", ownerUuid);
        root.add("incoming", serializeList(incoming));
        root.add("outgoing", serializeList(outgoing));
        Path tmp = target.resolveSibling(FILE_NAME + ".tmp");
        try (Writer w = new FileWriter(tmp.toFile())) {
            GSON.toJson(root, w);
        } catch (Exception ignored) { return; }
        try {
            java.nio.file.Files.move(tmp, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ignored) {}
    }

    private static JsonArray serializeList(List<PendingRequest> list) {
        JsonArray arr = new JsonArray();
        for (PendingRequest r : list) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", r.uuid());
            obj.addProperty("username", r.username());
            arr.add(obj);
        }
        return arr;
    }
}