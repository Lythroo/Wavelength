package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import com.lythroo.wavelength.Wavelength;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerFriendData {

    private static final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();
    private static final Map<UUID, String>    names   = new ConcurrentHashMap<>();

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_server_friends.json";

    private ServerFriendData() {}

    public static void load() {
        friends.clear();
        pending.clear();
        names.clear();
        Path path = savePath();
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            parseUuidSetMap(root.getAsJsonObject("friends"), friends);
            parseUuidSetMap(root.getAsJsonObject("pending"), pending);

            JsonObject namesObj = root.getAsJsonObject("names");
            if (namesObj != null) {
                for (Map.Entry<String, JsonElement> e : namesObj.entrySet()) {
                    try { names.put(UUID.fromString(e.getKey()), e.getValue().getAsString()); }
                    catch (Exception ignored) {}
                }
            }
            Wavelength.LOGGER.info("[Wavelength] Loaded friend data: {} friendships, {} pending requests.",
                    friends.size(), pending.size());
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load server friend data: {}", e.getMessage());
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.add("friends", serializeUuidSetMap(friends));
        root.add("pending", serializeUuidSetMap(pending));
        JsonObject namesObj = new JsonObject();
        names.forEach((k, v) -> namesObj.addProperty(k.toString(), v));
        root.add("names", namesObj);
        try (Writer w = new FileWriter(savePath().toFile())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to save server friend data: {}", e.getMessage());
        }
    }

    public static boolean areFriends(UUID a, UUID b) {
        Set<UUID> s = friends.get(a);
        return s != null && s.contains(b);
    }

    public static boolean hasPending(UUID from, UUID to) {
        Set<UUID> s = pending.get(from);
        return s != null && s.contains(to);
    }

    public static List<UUID> incomingRequestsFor(UUID player) {
        List<UUID> result = new ArrayList<>();
        pending.forEach((from, targets) -> {
            if (targets.contains(player)) result.add(from);
        });
        return result;
    }

    public static List<UUID> outgoingRequestsFrom(UUID sender) {
        Set<UUID> targets = pending.get(sender);
        if (targets == null || targets.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(targets);
    }

    public static List<Map.Entry<UUID, String>> getFriendList(UUID player) {
        Set<UUID> friendSet = friends.getOrDefault(player, Set.of());
        List<Map.Entry<UUID, String>> result = new ArrayList<>();
        for (UUID fUuid : friendSet) {
            result.add(Map.entry(fUuid, names.getOrDefault(fUuid, fUuid.toString().substring(0, 8))));
        }
        return result;
    }

    public static String getName(UUID uuid) {
        return names.getOrDefault(uuid, uuid.toString().substring(0, 8));
    }

    public static void updateName(UUID uuid, String username) {
        names.put(uuid, username);

    }

    public static boolean addRequest(UUID from, String fromName, UUID to) {
        if (areFriends(from, to) || hasPending(from, to)) return false;
        names.put(from, fromName);
        pending.computeIfAbsent(from, k -> ConcurrentHashMap.newKeySet()).add(to);
        save();
        return true;
    }

    public static boolean acceptRequest(UUID acceptor, String acceptorName, UUID requester) {
        if (!hasPending(requester, acceptor)) return false;
        Set<UUID> reqs = pending.get(requester);
        if (reqs != null) reqs.remove(acceptor);
        names.put(acceptor, acceptorName);
        friends.computeIfAbsent(acceptor,  k -> ConcurrentHashMap.newKeySet()).add(requester);
        friends.computeIfAbsent(requester, k -> ConcurrentHashMap.newKeySet()).add(acceptor);
        save();
        return true;
    }

    public static boolean declineRequest(UUID decliner, UUID requester) {
        Set<UUID> reqs = pending.get(requester);
        if (reqs == null || !reqs.remove(decliner)) return false;
        save();
        return true;
    }

    public static boolean cancelRequest(UUID sender, UUID target) {
        Set<UUID> reqs = pending.get(sender);
        if (reqs == null || !reqs.remove(target)) return false;
        save();
        return true;
    }

    public static boolean unfriend(UUID a, UUID b) {
        boolean changed = false;
        Set<UUID> aF = friends.get(a);
        if (aF != null) changed |= aF.remove(b);
        Set<UUID> bF = friends.get(b);
        if (bF != null) changed |= bF.remove(a);
        if (changed) save();
        return changed;
    }

    private static Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static void parseUuidSetMap(JsonObject obj, Map<UUID, Set<UUID>> map) {
        if (obj == null) return;
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            try {
                UUID key = UUID.fromString(entry.getKey());
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (JsonElement el : entry.getValue().getAsJsonArray()) {
                    try { set.add(UUID.fromString(el.getAsString())); }
                    catch (Exception ignored) {}
                }
                map.put(key, set);
            } catch (Exception ignored) {}
        }
    }

    private static JsonObject serializeUuidSetMap(Map<UUID, Set<UUID>> map) {
        JsonObject obj = new JsonObject();
        map.forEach((key, set) -> {
            if (set.isEmpty()) return;
            JsonArray arr = new JsonArray();
            set.forEach(v -> arr.add(v.toString()));
            obj.add(key.toString(), arr);
        });
        return obj;
    }
}