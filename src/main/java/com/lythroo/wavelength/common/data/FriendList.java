package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendList {

    public record Friend(String uuid, String username) {}

    private static final List<Friend> friends = new CopyOnWriteArrayList<>();
    private static String ownerUuid = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_friends.json";

    public static void onPlayerJoin(UUID localPlayerUuid) {
        String id = localPlayerUuid.toString();
        if (!id.equals(ownerUuid)) {
            friends.clear();
            ownerUuid = id;
            loadFromDisk();
        }
    }

    public static void clearAll() {
        friends.clear();
        ownerUuid = null;
    }

    public static List<Friend> getAll() {
        return Collections.unmodifiableList(friends);
    }

    public static boolean isFriend(UUID uuid) {
        String id = uuid.toString();
        return friends.stream().anyMatch(f -> f.uuid().equals(id));
    }

    public static int count() {
        return friends.size();
    }

    public static void add(UUID uuid, String username) {
        if (!isFriend(uuid)) {
            friends.add(new Friend(uuid.toString(), username));
            save();
        }
    }

    public static void remove(UUID uuid) {
        if (friends.removeIf(f -> f.uuid().equals(uuid.toString()))) {
            save();
        }
    }

    public static void replaceAll(List<String> uuids, List<String> usernames) {
        friends.clear();
        for (int i = 0; i < uuids.size(); i++) {
            friends.add(new Friend(uuids.get(i),
                    i < usernames.size() ? usernames.get(i) : uuids.get(i).substring(0, 8)));
        }
        save();
    }

    private static void loadFromDisk() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            String storedOwner = root.has("ownerUuid") ? root.get("ownerUuid").getAsString() : "";
            if (!storedOwner.equals(ownerUuid)) return;
            JsonArray arr = root.getAsJsonArray("friends");
            if (arr == null) return;
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                friends.add(new Friend(
                        obj.get("uuid").getAsString(),
                        obj.get("username").getAsString()));
            }
        } catch (Exception ignored) {}
    }

    private static void save() {
        if (ownerUuid == null) return;
        Path target = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        root.addProperty("ownerUuid", ownerUuid);
        JsonArray arr = new JsonArray();
        for (Friend f : friends) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", f.uuid());
            obj.addProperty("username", f.username());
            arr.add(obj);
        }
        root.add("friends", arr);
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
}