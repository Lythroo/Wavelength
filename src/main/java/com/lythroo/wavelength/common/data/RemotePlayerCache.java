package com.lythroo.wavelength.common.data;

import com.lythroo.wavelength.Wavelength;
import com.lythroo.wavelength.common.network.SyncPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemotePlayerCache {

    public record Entry(
            String     townName,
            Rank       rank,
            PrivacyMode privacy,
            boolean    pvpMode
    ) {

        public String rankLine() {
            return rank.format(townName);
        }
    }

    private static final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    private RemotePlayerCache() {}

    public static void put(SyncPacket.PlayerDataSyncPayload payload) {
        Rank rank;
        try {
            rank = Rank.valueOf(payload.rankName());
        } catch (IllegalArgumentException e) {
            rank = Rank.NONE;
        }

        PrivacyMode privacy;
        try {
            privacy = PrivacyMode.valueOf(payload.privacyName());
        } catch (IllegalArgumentException e) {
            privacy = PrivacyMode.PUBLIC;
        }

        Entry entry = new Entry(payload.townName(), rank, privacy, payload.pvpMode());
        cache.put(payload.playerUuid(), entry);

        Wavelength.LOGGER.debug(
                "[Wavelength] RemotePlayerCache: stored data for {} — town='{}' rank={} privacy={}",
                payload.playerUuid(), payload.townName(),
                payload.rankName(), payload.privacyName());
    }

    public static Entry get(UUID uuid) {
        return cache.get(uuid);
    }

    public static String rankLine(UUID uuid) {
        Entry e = cache.get(uuid);
        return e != null ? e.rankLine() : "";
    }

    public static PrivacyMode privacy(UUID uuid) {
        Entry e = cache.get(uuid);
        return e != null ? e.privacy() : PrivacyMode.PUBLIC;
    }

    public static boolean pvpMode(UUID uuid) {
        Entry e = cache.get(uuid);
        return e != null && e.pvpMode();
    }

    public static boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public static void clear() {
        cache.clear();
    }

    public static int size() {
        return cache.size();
    }
}