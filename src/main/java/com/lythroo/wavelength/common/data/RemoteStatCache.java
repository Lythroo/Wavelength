package com.lythroo.wavelength.common.data;

import com.lythroo.wavelength.common.network.SyncPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteStatCache {

    public static class Entry {
        public long blocksMined;
        public long blocksPlaced;
        public long mobsKilled;
        public long cropsHarvested;
        public long itemsCrafted;
        public long distanceTraveled;
        public long playtimeTicks;
    }

    private static final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    private RemoteStatCache() {}

    public static void put(SyncPacket.StatDataSyncPayload p) {
        Entry e = new Entry();
        e.blocksMined      = p.blocksMined();
        e.blocksPlaced     = p.blocksPlaced();
        e.mobsKilled       = p.mobsKilled();
        e.cropsHarvested   = p.cropsHarvested();
        e.itemsCrafted     = p.itemsCrafted();
        e.distanceTraveled = p.distanceTraveled();
        e.playtimeTicks    = p.playtimeTicks();
        cache.put(p.playerUuid(), e);
    }

    public static Entry get(UUID uuid)            { return cache.get(uuid); }
    public static boolean has(UUID uuid)          { return cache.containsKey(uuid); }
    public static void remove(UUID uuid)          { cache.remove(uuid); }
    public static void clear()                    { cache.clear(); }
}