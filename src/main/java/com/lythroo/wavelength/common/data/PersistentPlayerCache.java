package com.lythroo.wavelength.common.data;

import com.google.gson.*;
import com.lythroo.wavelength.Wavelength;
import com.lythroo.wavelength.common.network.SyncPacket;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PersistentPlayerCache {

    public static class Entry {

        public String  username      = "";
        public String  townName      = "";
        public String  rankName      = Rank.NONE.name();
        public String  privacyName   = PrivacyMode.PUBLIC.name();
        public boolean pvpMode       = false;

        public long    blocksMined      = 0;
        public long    blocksPlaced     = 0;
        public long    mobsKilled       = 0;
        public long    cropsHarvested   = 0;
        public long    itemsCrafted     = 0;
        public long    distanceTraveled = 0;
        public long    playtimeTicks    = 0;

        public String rankLine() {
            try { return Rank.valueOf(rankName).format(townName); }
            catch (Exception e) { return ""; }
        }

        public Rank rank() {
            try { return Rank.valueOf(rankName); } catch (Exception e) { return Rank.NONE; }
        }

        public PrivacyMode privacy() {
            try { return PrivacyMode.valueOf(privacyName); }
            catch (Exception e) { return PrivacyMode.PUBLIC; }
        }
    }

    private static final Map<UUID, Entry> cache     = new ConcurrentHashMap<>();
    private static       String           ownerUuid = null;

    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_known_players.json";

    private PersistentPlayerCache() {}

    public static void onPlayerJoin(UUID localPlayerUuid) {
        ownerUuid = localPlayerUuid.toString();
        loadFromDisk();
    }

    public static void onDisconnect() {

        ownerUuid = null;
    }

    public static void putIdentity(SyncPacket.PlayerDataSyncPayload p, String username) {
        UUID uuid = p.playerUuid();
        Entry e = cache.computeIfAbsent(uuid, k -> new Entry());
        if (username != null && !username.isBlank()) e.username = username;
        e.townName    = p.townName();
        e.rankName    = p.rankName();
        e.privacyName = p.privacyName();
        e.pvpMode     = p.pvpMode();
        save();
    }

    public static void putUsername(UUID uuid, String username) {
        if (username == null || username.isBlank()) return;
        cache.computeIfAbsent(uuid, k -> new Entry()).username = username;
        save();
    }

    public static void putStats(SyncPacket.StatDataSyncPayload p) {
        UUID uuid = p.playerUuid();
        Entry e = cache.computeIfAbsent(uuid, k -> new Entry());
        e.blocksMined      = p.blocksMined();
        e.blocksPlaced     = p.blocksPlaced();
        e.mobsKilled       = p.mobsKilled();
        e.cropsHarvested   = p.cropsHarvested();
        e.itemsCrafted     = p.itemsCrafted();
        e.distanceTraveled = p.distanceTraveled();
        e.playtimeTicks    = p.playtimeTicks();
        save();
    }

    public static Entry get(UUID uuid) { return cache.get(uuid); }

    public static boolean has(UUID uuid) { return cache.containsKey(uuid); }

    public static List<Map.Entry<UUID, Entry>> getAllExcept(UUID localUuid) {
        List<Map.Entry<UUID, Entry>> list = new ArrayList<>(cache.entrySet());
        list.removeIf(e -> e.getKey().equals(localUuid));
        list.sort(Comparator.comparing(e -> e.getValue().username));
        return list;
    }

    private static void loadFromDisk() {
        Path path = savePath();
        if (!path.toFile().exists()) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject players = root.getAsJsonObject("players");
            if (players == null) return;
            for (Map.Entry<String, JsonElement> kv : players.entrySet()) {
                try {
                    UUID  uuid = UUID.fromString(kv.getKey());
                    Entry e    = GSON.fromJson(kv.getValue(), Entry.class);
                    if (e != null) cache.put(uuid, e);
                } catch (Exception ignored) {}
            }
            Wavelength.LOGGER.info("[Wavelength] PersistentPlayerCache loaded {} entries.", cache.size());
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load player cache: {}", e.getMessage());
        }
    }

    private static void save() {
        JsonObject root    = new JsonObject();
        JsonObject players = new JsonObject();
        cache.forEach((uuid, entry) -> players.add(uuid.toString(), GSON.toJsonTree(entry)));
        root.add("players", players);
        Path target = savePath();
        Path tmp    = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer w = new FileWriter(tmp.toFile())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to write player cache tmp: {}", e.getMessage());
            return;
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to atomically replace player cache: {}", e.getMessage());
        }
    }

    private static Path savePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}